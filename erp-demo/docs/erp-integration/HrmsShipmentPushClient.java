package erp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * HRMS 开放接口客户端 —— ERP（Java / Spring Boot）出库流程自动推送运单
 * ============================================================================
 *
 * <b>配置</b>（application.yml，密钥只放服务端配置/环境变量，切勿写进前端）：
 * <pre>
 * hrms:
 *   push:
 *     base-url: http://hrms-host:8080/api
 *     api-key:  ${HRMS_API_KEY}   # 在 HRMS「物流跟踪 → 批量导入运单 → 开放接口」复制
 * </pre>
 *
 * <b>接入方式</b>：不要在出库业务事务里直接发 HTTP。出库单确认（拿到运单号）时，
 * 先在同一个本地事务里调用 {@code ShipmentPushOutbox.enqueue(...)} 登记，
 * 再由 {@code ShipmentPushOutbox.pushPending()} 定时补偿推送（见该类）。
 *
 * <b>幂等</b>：HRMS 以 trackingNo 去重，重复推送返回 skipped，重试不会产生重复发货单。
 *
 * <b>返回</b>：code=200 表示请求已受理，data 里 total/success/skipped/failed/errors 为逐行结果；
 * failed>0 说明该运单本身不合规（如订单号不存在），重试无意义，应转人工处理。
 */
@Component
public class HrmsShipmentPushClient {

    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${hrms.push.base-url}")
    private String baseUrl;

    @Value("${hrms.push.api-key}")
    private String apiKey;

    public HrmsShipmentPushClient() {
        // 连接 5s、读取 10s，避免 ERP 线程被外部接口拖死
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.rest = new RestTemplate(factory);
    }

    /** 推送单条运单，返回 HRMS 的统计结果 */
    public PushResponse push(ShipmentPushItem item) {
        String body = writeJson(Map.of("items", List.of(item.toMap())));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        headers.set("X-API-Key", apiKey);

        ResponseEntity<String> response = rest.exchange(
                baseUrl + "/open-api/import/shipment",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("HRMS 返回异常状态：" + response.getStatusCode());
        }
        try {
            JsonNode root = mapper.readTree(response.getBody());
            if (root.path("code").asInt(200) != 200) {
                throw new IllegalStateException("HRMS 返回错误：" + root.path("msg").asText());
            }
            JsonNode data = root.path("data");
            return new PushResponse(
                    data.path("total").asInt(0),
                    data.path("success").asInt(0),
                    data.path("skipped").asInt(0),
                    data.path("failed").asInt(0),
                    data.path("errors").toString());
        } catch (Exception e) {
            throw new IllegalStateException("HRMS 响应解析失败：" + e.getMessage(), e);
        }
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("报文序列化失败", e);
        }
    }

    /** HRMS 逐行统计结果 */
    @Data
    public static class PushResponse {
        private final int total;
        private final int success;
        private final int skipped;
        private final int failed;
        private final String errors;
    }
}
