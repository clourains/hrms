package com.erpdemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class OutboundPushJob {

    private static final Logger log = LoggerFactory.getLogger(OutboundPushJob.class);
    private static final int MAX_ATTEMPTS = 5;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String pushUrl;
    private final String apiKey;

    public OutboundPushJob(JdbcTemplate jdbcTemplate,
                           ObjectMapper objectMapper,
                           @Value("${hrms.push.base-url}") String baseUrl,
                           @Value("${hrms.push.api-key}") String apiKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.pushUrl = baseUrl.replaceAll("/+$", "") + "/open-api/import/shipment";

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Scheduled(fixedDelay = 20000, initialDelay = 5000)
    public void pushPending() {
        if (apiKey == null || apiKey.isBlank() || "REPLACE_WITH_API_KEY".equals(apiKey)) {
            log.warn("HRMS api-key 未配置，跳过本次推送");
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, payload, attempts
                FROM erp_shipment_push_outbox
                WHERE status = 'PENDING' AND next_retry_at <= CURRENT_TIMESTAMP
                ORDER BY id
                LIMIT 10
                """);
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("ID")).longValue();
            String payload = (String) row.get("PAYLOAD");
            int attempts = ((Number) row.get("ATTEMPTS")).intValue();
            pushOne(id, payload, attempts);
        }
    }

    private void pushOne(long id, String payload, int attempts) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            headers.setContentType(MediaType.valueOf("application/json; charset=UTF-8"));
            HttpEntity<byte[]> entity = new HttpEntity<>(payload.getBytes(StandardCharsets.UTF_8), headers);

            ResponseEntity<String> response = restTemplate.exchange(pushUrl, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody() == null ? "" : response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                failAttempt(id, attempts, "HTTP " + response.getStatusCode().value() + ": " + truncate(responseBody, 400));
                return;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            if (root.path("code").asInt() != 200) {
                failAttempt(id, attempts, "返回 code=" + root.path("code").asInt() + ": " + truncate(responseBody, 400));
                return;
            }

            JsonNode data = root.path("data");
            int failed = data.path("failed").asInt(0);
            if (failed > 0) {
                jdbcTemplate.update("""
                                UPDATE erp_shipment_push_outbox
                                SET status = 'FAILED', last_error = ?, next_retry_at = NULL
                                WHERE id = ?
                                """,
                        truncate(data.path("errors").toString(), 500), id);
                log.warn("运单推送部分失败 outboxId={} error={}", id, truncate(data.path("errors").toString(), 200));
            } else {
                jdbcTemplate.update("""
                                UPDATE erp_shipment_push_outbox
                                SET status = 'DONE', last_error = NULL, next_retry_at = NULL
                                WHERE id = ?
                                """, id);
                log.info("运单推送成功 outboxId={} success={} skipped={}",
                        id, data.path("success").asInt(0), data.path("skipped").asInt(0));
            }
        } catch (Exception e) {
            failAttempt(id, attempts, truncate(e.toString(), 500));
            log.warn("运单推送异常 outboxId={} error={}", id, e.toString());
        }
    }

    private void failAttempt(long id, int attempts, String error) {
        int nextAttempts = attempts + 1;
        String message = truncate(error, 500);
        if (nextAttempts >= MAX_ATTEMPTS) {
            jdbcTemplate.update("""
                            UPDATE erp_shipment_push_outbox
                            SET status = 'FAILED', attempts = ?, last_error = ?, next_retry_at = NULL
                            WHERE id = ?
                            """, nextAttempts, message, id);
            log.warn("运单推送达到最大重试次数，置为 FAILED outboxId={}", id);
        } else {
            Timestamp nextRetry = Timestamp.valueOf(LocalDateTime.now().plusMinutes(1L << nextAttempts));
            jdbcTemplate.update("""
                            UPDATE erp_shipment_push_outbox
                            SET status = 'PENDING', attempts = ?, last_error = ?, next_retry_at = ?
                            WHERE id = ?
                            """, nextAttempts, message, nextRetry, id);
            log.info("运单推送失败，将在 {} 重试 outboxId={}", nextRetry, id);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
