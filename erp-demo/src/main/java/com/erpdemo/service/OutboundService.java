package com.erpdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OutboundService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmm");

    private static final String LIST_SQL = """
            SELECT o.id, o.outbound_no, o.tracking_no, o.carrier, o.transport_type,
                   o.port_from, o.port_to, o.dest_country, o.order_no, o.ship_date,
                   o.status, o.create_time,
                   p.status AS push_status, p.attempts AS push_attempts, p.last_error AS push_last_error
            FROM erp_outbound o
            LEFT JOIN erp_shipment_push_outbox p ON p.outbound_id = o.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboundService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> list() {
        return jdbcTemplate.query(LIST_SQL + " ORDER BY o.id DESC", this::mapRow);
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String trackingNo = text(body, "trackingNo");
        if (trackingNo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "运单号不能为空");
        }
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM erp_outbound WHERE tracking_no = ?", Integer.class, trackingNo);
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "运单号「" + trackingNo + "」已存在");
        }

        LocalDate shipDate = parseDate(text(body, "shipDate"), "发货日期");
        String outboundNo = nextOutboundNo();
        String transportType = text(body, "transportType");

        jdbcTemplate.update("""
                        INSERT INTO erp_outbound
                            (outbound_no, carrier, tracking_no, transport_type, port_from, port_to,
                             dest_country, order_no, ship_date, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'WAIT')
                        """,
                outboundNo,
                emptyToNull(text(body, "carrier")),
                trackingNo,
                emptyToNull(transportType),
                emptyToNull(text(body, "portFrom")),
                emptyToNull(text(body, "portTo")),
                emptyToNull(text(body, "destCountry")),
                emptyToNull(text(body, "orderNo")),
                shipDate == null ? null : java.sql.Date.valueOf(shipDate));

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM erp_outbound WHERE outbound_no = ?", Long.class, outboundNo);
        return findById(id);
    }

    @Transactional
    public Map<String, Object> confirm(long id) {
        return confirm(id, null);
    }

    /**
     * 确认出库并写入推送队列，remark 为空时沿用默认出库单备注。
     */
    @Transactional
    public Map<String, Object> confirm(long id, String remark) {
        Map<String, Object> outbound = findById(id);
        if (outbound == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "出库单不存在");
        }
        if (!"WAIT".equals(outbound.get("status"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅「待出库」状态的出库单可确认");
        }

        jdbcTemplate.update("UPDATE erp_outbound SET status = 'CONFIRMED' WHERE id = ? AND status = 'WAIT'", id);

        // 同一运单只入队一次：已存在 outbox 时不再重复写入
        Integer queued = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM erp_shipment_push_outbox WHERE tracking_no = ?",
                Integer.class, outbound.get("trackingNo"));
        if (queued == null || queued == 0) {
            String payload = buildPayload(outbound, remark);
            jdbcTemplate.update("""
                            INSERT INTO erp_shipment_push_outbox
                                (outbound_id, tracking_no, payload, status, attempts, next_retry_at)
                            VALUES (?, ?, ?, 'PENDING', 0, CURRENT_TIMESTAMP)
                            """,
                    id, outbound.get("trackingNo"), payload);
        }
        return findById(id);
    }

    public Map<String, Object> retry(long id) {
        Map<String, Object> outbound = findById(id);
        if (outbound == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "出库单不存在");
        }
        int updated = jdbcTemplate.update("""
                        UPDATE erp_shipment_push_outbox
                        SET status = 'PENDING', attempts = 0, last_error = NULL, next_retry_at = CURRENT_TIMESTAMP
                        WHERE outbound_id = ? AND status = 'FAILED'
                        """, id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅推送失败的单可重试");
        }
        return findById(id);
    }

    private Map<String, Object> findById(long id) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                LIST_SQL + " WHERE o.id = ?", this::mapRow, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getLong("id"));
        item.put("outboundNo", rs.getString("outbound_no"));
        item.put("trackingNo", rs.getString("tracking_no"));
        item.put("carrier", rs.getString("carrier"));
        item.put("transportType", rs.getString("transport_type"));
        item.put("portFrom", rs.getString("port_from"));
        item.put("portTo", rs.getString("port_to"));
        item.put("destCountry", rs.getString("dest_country"));
        item.put("orderNo", rs.getString("order_no"));
        item.put("shipDate", rs.getObject("ship_date", LocalDate.class));
        item.put("status", rs.getString("status"));
        item.put("createTime", rs.getObject("create_time", LocalDateTime.class));
        item.put("pushStatus", rs.getString("push_status"));
        item.put("attempts", rs.getInt("push_attempts"));
        item.put("lastError", rs.getString("push_last_error"));
        return item;
    }

    private String buildPayload(Map<String, Object> outbound, String remark) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("trackingNo", outbound.get("trackingNo"));
        item.put("carrier", outbound.get("carrier"));
        item.put("transportType", outbound.get("transportType"));
        item.put("portFrom", outbound.get("portFrom"));
        item.put("portTo", outbound.get("portTo"));
        item.put("shipDate", outbound.get("shipDate") == null ? null : outbound.get("shipDate").toString());
        // ERP 订单号仅本地留存：HRMS 只接受其系统内已存在的订单号，外部平台单号直接上报会导致整单推送失败
        item.put("remark", remark == null || remark.isBlank()
                ? "ERP出库单:" + outbound.get("outboundNo") : remark);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", List.of(item));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "构造推送报文失败");
        }
    }

    private String nextOutboundNo() {
        String prefix = "OUT" + LocalDateTime.now().format(NO_FORMAT);
        for (int seq = 1; seq <= 999; seq++) {
            String candidate = prefix + String.format("%03d", seq);
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM erp_outbound WHERE outbound_no = ?", Integer.class, candidate);
            if (count == null || count == 0) {
                return candidate;
            }
        }
        return prefix + System.currentTimeMillis() % 1000;
    }

    private LocalDate parseDate(String value, String label) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "格式应为 yyyy-MM-dd");
        }
    }

    private String text(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
