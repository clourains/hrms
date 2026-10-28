package erp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * 运单推送 Outbox + 定时补偿
 * ============================================================================
 *
 * 为什么需要它：出库确认时直接发 HTTP 会把外部接口的抖动带进出库事务；
 * 用本表先登记（本地事务），再由定时任务推送，失败可重试且不会丢单。
 *
 * <b>建表 DDL（在 ERP 库执行一次）</b>：
 * <pre>
 * CREATE TABLE erp_shipment_push_outbox (
 *   id            BIGINT AUTO_INCREMENT PRIMARY KEY,
 *   outbound_no   VARCHAR(64)  NOT NULL COMMENT 'ERP 出库单号',
 *   tracking_no   VARCHAR(100) NOT NULL COMMENT '运单号（唯一幂等键）',
 *   payload       TEXT         NOT NULL COMMENT '推送报文 JSON',
 *   status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/FAILED',
 *   attempts      INT          NOT NULL DEFAULT 0,
 *   last_error    VARCHAR(500) NULL,
 *   next_retry_at DATETIME     NOT NULL,
 *   create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *   update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 *   UNIQUE KEY uk_tracking (tracking_no),
 *   KEY idx_status_retry (status, next_retry_at)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * </pre>
 *
 * <b>接入点</b>（出库单确认 / 拿到运单号之后，与出库记录同一事务）：
 * <pre>
 * &#64;Transactional
 * public void confirmOutbound(OutboundOrder outbound) {
 *     // ... 原有出库逻辑、扣库存、生成运单号 ...
 *     outbox.enqueue(outbound.getOutboundNo(),
 *         ShipmentPushItem.builder().trackingNo(waybillNo)...build());
 * }
 * </pre>
 *
 * 重试策略：网络/服务异常按 2/4/8/16 分钟退避，最多 5 次；HRMS 校验失败（failed&gt;0）
 * 说明数据本身有问题，直接标 FAILED 转人工，不浪费重试。
 */
@Component
public class ShipmentPushOutbox {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 50;

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final HrmsShipmentPushClient client;

    public ShipmentPushOutbox(JdbcTemplate jdbc, ObjectMapper mapper, HrmsShipmentPushClient client) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.client = client;
    }

    /** 在出库业务事务内登记；tracking_no 唯一索引保证同一运单不会重复入队 */
    public void enqueue(String outboundNo, ShipmentPushItem item) {
        String payload = writeJson(item.toMap());
        jdbc.update("""
                INSERT INTO erp_shipment_push_outbox
                    (outbound_no, tracking_no, payload, status, attempts, next_retry_at)
                VALUES (?, ?, ?, 'PENDING', 0, NOW())
                ON DUPLICATE KEY UPDATE payload = VALUES(payload)
                """,
                outboundNo, item.getTrackingNo(), payload);
    }

    /** 每 30 秒扫一次待推送记录，逐条推送（单条失败不影响其他人） */
    @Scheduled(fixedDelay = 30_000)
    public void pushPending() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, tracking_no, payload, attempts
                FROM erp_shipment_push_outbox
                WHERE status = 'PENDING' AND next_retry_at <= NOW()
                ORDER BY id LIMIT ?
                """, BATCH_SIZE);

        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            int attempts = ((Number) row.get("attempts")).intValue();
            try {
                ShipmentPushItem item = mapper.readValue(String.valueOf(row.get("payload")), ShipmentPushItem.class);
                HrmsShipmentPushClient.PushResponse result = client.push(item);

                if (result.getFailed() > 0) {
                    // 数据不合规（如关联订单号不存在），重试无意义
                    markFailed(id, attempts + 1, "HRMS 校验失败：" + result.getErrors());
                } else {
                    // success=1 或 skipped=1（HRMS 已有该运单）都视为完成
                    jdbc.update("UPDATE erp_shipment_push_outbox SET status='DONE', attempts=?, last_error=NULL WHERE id=?",
                            attempts + 1, id);
                }
            } catch (Exception e) {
                int nextAttempts = attempts + 1;
                String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                if (nextAttempts >= MAX_ATTEMPTS) {
                    markFailed(id, nextAttempts, error);
                } else {
                    long delaySeconds = (long) Math.pow(2, nextAttempts) * 60;
                    jdbc.update("""
                            UPDATE erp_shipment_push_outbox
                            SET attempts=?, last_error=?, next_retry_at=DATE_ADD(NOW(), INTERVAL ? SECOND)
                            WHERE id=?
                            """, nextAttempts, truncate(error, 500), delaySeconds, id);
                }
            }
        }
    }

    private void markFailed(Long id, int attempts, String error) {
        jdbc.update("UPDATE erp_shipment_push_outbox SET status='FAILED', attempts=?, last_error=? WHERE id=?",
                attempts, truncate(error, 500), id);
    }

    private String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() > max ? text.substring(0, max) : text;
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("报文序列化失败", e);
        }
    }
}
