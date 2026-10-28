package com.hrms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.config.AiConfig;
import com.hrms.dto.BusinessInsightDTO.*;
import com.hrms.service.BusinessInsightService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 业务侧智能分析实现
 *
 * 规则通道只做规则计算，不联网；全部数值来自真实业务表：
 * biz_order（订单，金额字段 total_amount_usd，下单日期 order_date）、
 * biz_order_item（明细，发货仓库 warehouse_id、数量 quantity）、
 * biz_customer（客户名称 company_name、国家 country）、
 * biz_product（SKU、产品名）、biz_inventory（库存 quantity）、
 * biz_warehouse（仓库名）、biz_shipment（ETA、承运商、状态）、
 * biz_shipment_track（轨迹时间 track_time）。
 *
 * 订单有效口径：deleted = 0 且 status BETWEEN 1 AND 5，
 * 即已确认(1)/备货中(2)/已发货(3)/已签收(4)/已完成(5)；排除待确认(0)与已取消(9)。
 */
@Slf4j
@Service
public class BusinessInsightServiceImpl implements BusinessInsightService {

    /** 预测外推月数 */
    private static final int FORECAST_MONTHS = 3;
    /** 回归样本月数 */
    private static final int HISTORY_MONTHS = 12;
    /** 客户流失：中风险阈值（天） */
    private static final int CUSTOMER_MEDIUM_DAYS = 90;
    /** 客户流失：高风险阈值（天） */
    private static final int CUSTOMER_HIGH_DAYS = 180;
    /** 补货：高风险阈值（可售天数） */
    private static final int STOCK_HIGH_DAYS = 15;
    /** 补货：中风险阈值（可售天数） */
    private static final int STOCK_MEDIUM_DAYS = 30;
    /** 物流：轨迹停滞阈值（天） */
    private static final int TRACK_STALE_DAYS = 7;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ==================== AI 生成通道参数 ====================
    /** AI 结果缓存 key（最新一条，全公司口径一致） */
    private static final String CACHE_KEY = "hrms:analytics:business-insight:latest";
    /** 缓存有效期 24 小时 */
    private static final long CACHE_TTL_HOURS = 24L;
    private static final long CACHE_TTL_MS = CACHE_TTL_HOURS * 60 * 60 * 1000L;
    private static final int MIN_INSIGHTS = 3;
    private static final int MAX_INSIGHTS = 6;
    private static final int MAX_TITLE_LEN = 16;
    private static final int MAX_DESC_LEN = 90;
    private static final int MAX_SUGGESTION_LEN = 120;
    private static final int MAX_COMMENT_LEN = 120;
    private static final int AI_CONNECT_TIMEOUT_MS = 5000;
    private static final int AI_READ_TIMEOUT_MS = 30000;
    private static final Set<String> VALID_TYPES = Set.of("warning", "success", "info");
    private static final Set<String> VALID_CATEGORIES = Set.of("sales", "customer", "stock", "logistics");

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是资深跨境贸易经营分析师，负责向管理层提交精简的业务智能洞察。",
            "输入是一份来自真实业务数据库的聚合事实（JSON），其中「规则洞察原文」是规则引擎的初步判断，可参考但不要照抄。",
            "输出要求：",
            "1. 只输出严格 JSON，不要 markdown 代码块，不要任何解释性文字；",
            "2. 结构：{\"insights\":[{\"title\":\"...\",\"type\":\"warning|success|info\",\"category\":\"sales|customer|stock|logistics\",\"description\":\"...\",\"suggestion\":\"...\"}],\"forecastComment\":\"...\"}",
            "3. insights 共 3-6 条，按重要程度从高到低排序；",
            "4. title 不超过 16 个汉字；description 控制在 60-90 字，必须引用事实中的具体数字；",
            "5. suggestion 写成「建议：1. xxx；2. xxx」的形式；",
            "6. type 只能取 warning、success、info；category 只能取 sales、customer、stock、logistics；",
            "7. 只能依据 facts 中的数字，严禁编造事实中不存在的数字，某项数据缺失时直接跳过该维度；",
            "8. forecastComment 是对未来 3 个月销售预测的一句话解读，50 字以内，需引用预测值或增长率。");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AiConfig aiConfig;

    /** Redis 不可用时（开发环境常见）退化为内存缓存，不影响接口 */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 生成串行锁：防止并发点击「AI 生成」重复调用模型、重复计费 */
    private final Object generateLock = new Object();

    /** 内存兜底缓存（Redis 不可用时使用） */
    private volatile BusinessInsights memoryCache;
    private volatile long memoryCacheAt;

    @Override
    public BusinessInsights getBusinessInsights(boolean generate) {
        if (!generate) {
            // 默认通道：规则结果；存在 24h 内的 AI 结果时直接回放
            BusinessInsights cached = loadCached();
            if (cached != null) {
                cached.setElapsedMs(0L);
                cached.setAiEnabled(aiEnabled());
                return cached;
            }
            long start = System.currentTimeMillis();
            return ruleResponse(buildRuleSnapshot(), start);
        }
        // 用户点「AI 生成」：跳过读取，强制重算；并发点击时串行执行
        synchronized (generateLock) {
            long start = System.currentTimeMillis();
            RuleSnapshot snapshot = buildRuleSnapshot();
            if (!aiEnabled()) {
                log.warn("业务洞察-AI 生成跳过：未配置 AI Key");
                return ruleResponse(snapshot, start);
            }
            try {
                Map<String, Object> facts = buildFacts(snapshot);
                String content = callModel(facts);
                ParsedOutput parsed = parseModelOutput(content);
                BusinessInsights result = snapshot.result;
                result.setSource("model");
                result.setModel(aiConfig.getModel());
                result.setAiEnabled(true);
                result.setGeneratedAt(LocalDateTime.now().format(DATETIME_FMT));
                result.setElapsedMs(System.currentTimeMillis() - start);
                result.getForecast().setComment(parsed.forecastComment);
                result.setInsights(parsed.insights);
                store(result);
                log.info("业务洞察-AI 生成成功：model={}, items={}, 耗时={}ms",
                        aiConfig.getModel(), parsed.insights.size(), result.getElapsedMs());
                return result;
            } catch (Exception e) {
                log.warn("业务洞察-AI 生成失败，回退规则结果：{}", brief(e));
                BusinessInsights fallback = ruleResponse(snapshot, start);
                fallback.setAiError(brief(e));
                return fallback;
            }
        }
    }

    /** 规则结果组装：source=rule、model 为空，保留规则计算耗时 */
    private BusinessInsights ruleResponse(RuleSnapshot snapshot, long start) {
        BusinessInsights result = snapshot.result;
        result.setSource("rule");
        result.setModel(null);
        result.setAiEnabled(aiEnabled());
        if (result.getForecast() != null) {
            result.getForecast().setComment(null);
        }
        result.setElapsedMs(Math.max(0L, System.currentTimeMillis() - start));
        return result;
    }

    /** AiConfig.enabled 且 api-key 非空时才算可用 */
    private boolean aiEnabled() {
        return Boolean.TRUE.equals(aiConfig.getEnabled())
                && aiConfig.getApiKey() != null && !aiConfig.getApiKey().isBlank();
    }

    /** 规则计算快照：结果 + 近 12 个月销售额（供模型 facts 使用，避免重复查库） */
    private static class RuleSnapshot {
        private BusinessInsights result;
        private Map<String, Double> monthlySales = Collections.emptyMap();
    }

    /** 规则通道：原 getBusinessInsights 的计算逻辑，保持默认与兜底行为不变 */
    private RuleSnapshot buildRuleSnapshot() {
        BusinessInsights result = new BusinessInsights();
        result.setSource("rule");
        result.setGeneratedAt(LocalDateTime.now().format(DATETIME_FMT));
        RuleSnapshot snapshot = new RuleSnapshot();
        snapshot.result = result;
        LocalDate today = LocalDate.now();

        // 各维度独立兜底：单个维度查询异常时只让该维度为空，接口整体不报 500
        Map<String, Double> monthlySales = Collections.emptyMap();
        try {
            monthlySales = loadMonthlySales(today);
        } catch (Exception e) {
            log.warn("业务洞察-按月销售额查询失败：{}", e.getMessage());
        }
        snapshot.monthlySales = monthlySales;

        SalesForecast forecast = new SalesForecast();
        try {
            forecast = buildForecast(today, monthlySales);
        } catch (Exception e) {
            log.warn("业务洞察-销售预测失败：{}", e.getMessage());
        }
        result.setForecast(forecast);

        List<CustomerRisk> customerRisk = Collections.emptyList();
        try {
            customerRisk = buildCustomerRisk(today);
        } catch (Exception e) {
            log.warn("业务洞察-客户风险查询失败：{}", e.getMessage());
        }
        result.setCustomerRisk(customerRisk);

        List<RestockSuggestion> allRestock = Collections.emptyList();
        try {
            allRestock = buildRestock(today);
        } catch (Exception e) {
            log.warn("业务洞察-库存补货查询失败：{}", e.getMessage());
        }
        // 补货建议：只输出可售天数低于 30 天的（库存为 0 也纳入，此时 daysOfSupply 为 0）
        List<RestockSuggestion> restock = allRestock.stream()
                .filter(r -> r.getDaysOfSupply() != null && r.getDaysOfSupply() < STOCK_MEDIUM_DAYS)
                .limit(10)
                .toList();
        result.setRestock(restock);

        List<LogisticsAlert> allAlerts = Collections.emptyList();
        try {
            allAlerts = buildLogisticsAlerts(today);
        } catch (Exception e) {
            log.warn("业务洞察-物流异常查询失败：{}", e.getMessage());
        }
        result.setLogisticsAlerts(allAlerts);

        try {
            result.setInsights(buildInsights(today, monthlySales, forecast, customerRisk, allRestock, allAlerts));
        } catch (Exception e) {
            log.warn("业务洞察-洞察生成失败：{}", e.getMessage());
        }
        return snapshot;
    }

    // ==================== 销售预测 ====================

    /**
     * 最近 12 个月按月销售额（USD）。
     * 月份键 yyyy-MM；当月未结束也会计入，属于真实的「截至今日」口径。
     */
    private Map<String, Double> loadMonthlySales(LocalDate today) {
        LocalDate start = YearMonth.from(today).minusMonths(HISTORY_MONTHS - 1).atDay(1);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(order_date, '%Y-%m') AS m, SUM(total_amount_usd) AS amt "
                        + "FROM biz_order "
                        + "WHERE deleted = 0 AND status BETWEEN 1 AND 5 AND order_date >= ? AND order_date <= ? "
                        + "GROUP BY m",
                Date.valueOf(start), Date.valueOf(today));
        Map<String, Double> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object m = row.get("m");
            if (m == null) continue;
            map.put(String.valueOf(m), asDouble(row.get("amt")));
        }
        return map;
    }

    /**
     * 销售额预测：有效数据月份 >= 9 时用近 12 个月线性回归；
     * 否则只用「首个有数据月至今」的样本外推，并在样本不足时标注。
     * 预测值不得为负；为避免历史缺失导致的陡峭外推，预测值限制在最近 3 个月均值的 65%~135%。
     * growthRate =（未来 3 个月均值 / 最近 3 个月均值 - 1）× 100。
     */
    private SalesForecast buildForecast(LocalDate today, Map<String, Double> monthlySales) {
        SalesForecast forecast = new SalesForecast();
        YearMonth current = YearMonth.from(today);

        double[] y = new double[HISTORY_MONTHS];
        int coverage = 0;
        int firstDataIdx = -1;
        for (int i = 0; i < HISTORY_MONTHS; i++) {
            String key = current.minusMonths(HISTORY_MONTHS - 1 - i).format(MONTH_FMT);
            Double v = monthlySales.get(key);
            y[i] = v == null ? 0 : v;
            if (y[i] > 0) {
                coverage++;
                if (firstDataIdx < 0) {
                    firstDataIdx = i;
                }
            }
        }
        // 窗口内没有任何有效订单：返回空预测对象，不编造趋势
        if (coverage == 0) {
            return forecast;
        }

        // 样本不足时从首个有数据月开始拟合，避免把缺失月份当作真实零销售
        int start = coverage >= 9 ? 0 : Math.max(0, firstDataIdx);
        int n = HISTORY_MONTHS - start;
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = start; i < HISTORY_MONTHS; i++) {
            sumX += i;
            sumY += y[i];
            sumXY += (double) i * y[i];
            sumXX += (double) i * i;
        }
        double denom = n * sumXX - sumX * sumX;
        double slope = denom == 0 ? 0 : (n * sumXY - sumX * sumY) / denom;

        double last3Avg = (y[HISTORY_MONTHS - 3] + y[HISTORY_MONTHS - 2] + y[HISTORY_MONTHS - 1]) / 3.0;
        // 用最近 3 个月与前 3 个月均值的差推算月度趋势；样本不足时对趋势做减半阻尼，
        // 且单月变动不超过基期的 15%，避免数据缺口造成陡峭外推
        double perMonth = slope;
        if (start <= HISTORY_MONTHS - 6) {
            double prev3Avg = (y[HISTORY_MONTHS - 6] + y[HISTORY_MONTHS - 5] + y[HISTORY_MONTHS - 4]) / 3.0;
            perMonth = (last3Avg - prev3Avg) / 3.0;
        }
        double damped = perMonth * 0.5;
        boolean clamped = false;
        if (last3Avg > 0) {
            double maxStep = last3Avg * 0.15;
            if (damped > maxStep) {
                damped = maxStep;
                clamped = true;
            } else if (damped < -maxStep) {
                damped = -maxStep;
                clamped = true;
            }
        }

        List<String> months = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (int i = 1; i <= FORECAST_MONTHS; i++) {
            months.add(current.plusMonths(i).format(MONTH_FMT));
            double predicted = last3Avg + damped * i;
            values.add(round(Math.max(0, predicted), 1));
        }

        String method = coverage >= 9
                ? "近12个月趋势外推"
                : String.format("近%d个月趋势外推（样本不足）", coverage);
        if (clamped) {
            method += "·趋势平滑";
        }
        forecast.setMethod(method);
        forecast.setMonths(months);
        forecast.setValues(values);
        forecast.setSampleMonths(coverage);

        double future3Avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double growth = last3Avg <= 0 ? 0 : (future3Avg / last3Avg - 1) * 100;
        forecast.setGrowthRate(round(growth, 1));
        return forecast;
    }

    // ==================== 客户流失预警 ====================

    /**
     * 有订单客户按最后下单日期计算流失天数：>=180 高、>=90 中。
     * 按历史有效销售额倒序取前 10。
     */
    private List<CustomerRisk> buildCustomerRisk(LocalDate today) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.company_name AS name, c.country AS country, MAX(o.order_date) AS last_date, "
                        + "COUNT(*) AS order_count, SUM(o.total_amount_usd) AS total_usd "
                        + "FROM biz_order o JOIN biz_customer c ON c.id = o.customer_id "
                        + "WHERE o.deleted = 0 AND o.status BETWEEN 1 AND 5 "
                        + "GROUP BY o.customer_id, c.company_name, c.country "
                        + "HAVING last_date <= ? "
                        + "ORDER BY total_usd DESC LIMIT 10",
                Date.valueOf(today.minusDays(CUSTOMER_MEDIUM_DAYS)));

        List<CustomerRisk> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            LocalDate lastOrderDate = toLocalDate(row.get("last_date"));
            if (lastOrderDate == null) continue;
            int daysSince = (int) ChronoUnit.DAYS.between(lastOrderDate, today);
            if (daysSince < CUSTOMER_MEDIUM_DAYS) continue;

            int orderCount = (int) asDouble(row.get("order_count"));
            double totalSales = asDouble(row.get("total_usd"));

            CustomerRisk item = new CustomerRisk();
            item.setCustomerName(row.get("name") == null ? "未知客户" : String.valueOf(row.get("name")));
            item.setCountry(row.get("country") == null ? "" : String.valueOf(row.get("country")));
            item.setLastOrderDate(lastOrderDate.format(DATE_FMT));
            item.setDaysSince(daysSince);
            item.setOrderCount(orderCount);
            item.setTotalSalesUsd(BigDecimal.valueOf(totalSales).setScale(2, java.math.RoundingMode.HALF_UP));
            item.setLevel(daysSince >= CUSTOMER_HIGH_DAYS ? "high" : "medium");
            item.setReason(String.format("最后下单 %d 天前、历史 %d 单、金额 $%s",
                    daysSince, orderCount, String.format("%,.2f", totalSales)));
            list.add(item);
        }
        return list;
    }

    // ==================== 库存补货 ====================

    /**
     * 近 90 天出库销量按「订单明细发货仓库 + 产品」归属，折合月均销量后计算可售天数。
     * 只会评估近 90 天真实有出库的产品；库存为 0 时 daysOfSupply = 0，同样纳入。
     * 返回全部有销量（降序按可售天数升序排列）的候选，由调用方按阈值过滤。
     */
    private List<RestockSuggestion> buildRestock(LocalDate today) {
        List<Map<String, Object>> salesRows = jdbcTemplate.queryForList(
                "SELECT oi.product_id AS pid, oi.warehouse_id AS wid, SUM(oi.quantity) AS qty "
                        + "FROM biz_order_item oi JOIN biz_order o ON o.id = oi.order_id "
                        + "WHERE o.deleted = 0 AND o.status BETWEEN 1 AND 5 "
                        + "AND o.order_date >= ? AND oi.warehouse_id IS NOT NULL "
                        + "GROUP BY oi.product_id, oi.warehouse_id",
                Date.valueOf(today.minusDays(90)));
        if (salesRows.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, String> productSku = new HashMap<>();
        Map<Long, String> productName = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT id, sku, name FROM biz_product WHERE deleted = 0")) {
            long id = (long) asDouble(row.get("id"));
            productSku.put(id, row.get("sku") == null ? "" : String.valueOf(row.get("sku")));
            productName.put(id, row.get("name") == null ? "" : String.valueOf(row.get("name")));
        }
        Map<Long, String> warehouseName = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT id, warehouse_name FROM biz_warehouse WHERE deleted = 0")) {
            warehouseName.put((long) asDouble(row.get("id")),
                    row.get("warehouse_name") == null ? "" : String.valueOf(row.get("warehouse_name")));
        }
        Map<String, Integer> stockMap = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT product_id, warehouse_id, quantity FROM biz_inventory")) {
            long pid = (long) asDouble(row.get("product_id"));
            long wid = (long) asDouble(row.get("warehouse_id"));
            stockMap.put(pid + ":" + wid, (int) asDouble(row.get("quantity")));
        }

        List<RestockSuggestion> list = new ArrayList<>();
        for (Map<String, Object> row : salesRows) {
            long pid = (long) asDouble(row.get("pid"));
            long wid = (long) asDouble(row.get("wid"));
            if (!productSku.containsKey(pid)) continue;   // 产品已删除的不纳入
            double qty90 = asDouble(row.get("qty"));
            if (qty90 <= 0) continue;

            int stock = stockMap.getOrDefault(pid + ":" + wid, 0);
            double monthlySales = qty90 / 3.0;
            double dailySales = qty90 / 90.0;
            int daysOfSupply = dailySales > 0 ? (int) Math.round(stock / dailySales) : Integer.MAX_VALUE;
            int suggestedQty = (int) Math.max(0, Math.ceil(monthlySales * 2 - stock));

            RestockSuggestion item = new RestockSuggestion();
            item.setProductName(productName.getOrDefault(pid, ""));
            item.setSku(productSku.get(pid));
            item.setWarehouse(warehouseName.getOrDefault(wid, "未知仓库"));
            item.setStock(stock);
            item.setMonthlySales(round(monthlySales, 1));
            item.setDaysOfSupply(daysOfSupply);
            item.setSuggestedQty(suggestedQty);
            item.setLevel(daysOfSupply < STOCK_HIGH_DAYS ? "high"
                    : daysOfSupply < STOCK_MEDIUM_DAYS ? "medium" : null);
            list.add(item);
        }
        list.sort(Comparator.comparingInt(RestockSuggestion::getDaysOfSupply));
        return list;
    }

    // ==================== 物流异常 ====================

    /**
     * 未签收发货单（status 2 运输中 / 3 清关中）：
     * ETA 已过 → daysOverdue > 0；最新轨迹超过 7 天 → daysSinceTrack > 7；任一满足即告警。
     */
    private List<LogisticsAlert> buildLogisticsAlerts(LocalDate today) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.shipment_no AS shipment_no, s.tracking_no AS tracking_no, s.carrier AS carrier, "
                        + "s.eta AS eta, s.status AS status, "
                        + "(SELECT MAX(t.track_time) FROM biz_shipment_track t WHERE t.shipment_id = s.id) AS last_track "
                        + "FROM biz_shipment s "
                        + "WHERE s.deleted = 0 AND s.status IN (2, 3)");

        List<LogisticsAlert> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            LocalDate eta = toLocalDate(row.get("eta"));
            LocalDateTime lastTrack = toLocalDateTime(row.get("last_track"));
            int daysOverdue = eta != null && eta.isBefore(today)
                    ? (int) ChronoUnit.DAYS.between(eta, today) : 0;
            Integer daysSinceTrack = lastTrack == null ? null
                    : (int) ChronoUnit.DAYS.between(lastTrack.toLocalDate(), today);

            boolean overdue = daysOverdue > 0;
            boolean stale = daysSinceTrack != null && daysSinceTrack > TRACK_STALE_DAYS;
            if (!overdue && !stale) continue;

            List<String> parts = new ArrayList<>();
            if (overdue) parts.add("ETA 已超期 " + daysOverdue + " 天");
            if (stale) parts.add("最新轨迹 " + daysSinceTrack + " 天未更新");

            LogisticsAlert item = new LogisticsAlert();
            item.setShipmentNo(row.get("shipment_no") == null ? "" : String.valueOf(row.get("shipment_no")));
            item.setTrackingNo(row.get("tracking_no") == null ? "" : String.valueOf(row.get("tracking_no")));
            item.setCarrier(row.get("carrier") == null ? "" : String.valueOf(row.get("carrier")));
            item.setStatus((int) asDouble(row.get("status")));
            item.setEta(eta == null ? null : eta.format(DATE_FMT));
            item.setDaysOverdue(daysOverdue);
            item.setDaysSinceTrack(daysSinceTrack);
            item.setIssue(String.join("；", parts));
            list.add(item);
        }
        // 严重度：超期天数权重更高，其次轨迹停滞天数
        list.sort(Comparator.comparingInt((LogisticsAlert a) ->
                a.getDaysOverdue() * 2 + (a.getDaysSinceTrack() == null ? 0 : a.getDaysSinceTrack())).reversed());
        return list.size() > 10 ? new ArrayList<>(list.subList(0, 10)) : list;
    }

    // ==================== 洞察列表 ====================

    /**
     * 由上面四组规则结果生成 4-6 条中文洞察，全部引用真实数字；无数据的维度不生成。
     */
    private List<Insight> buildInsights(LocalDate today,
                                        Map<String, Double> monthlySales,
                                        SalesForecast forecast,
                                        List<CustomerRisk> customerRisk,
                                        List<RestockSuggestion> allRestock,
                                        List<LogisticsAlert> alerts) {
        List<Insight> list = new ArrayList<>();

        // 1. 销售预测
        if (forecast != null && forecast.getMonths() != null && !forecast.getMonths().isEmpty()
                && forecast.getValues() != null && !forecast.getValues().isEmpty()) {
            YearMonth current = YearMonth.from(today);
            double last3Avg = (monthlySales.getOrDefault(current.minusMonths(2).format(MONTH_FMT), 0.0)
                    + monthlySales.getOrDefault(current.minusMonths(1).format(MONTH_FMT), 0.0)
                    + monthlySales.getOrDefault(current.format(MONTH_FMT), 0.0)) / 3.0;
            double forecastSum = forecast.getValues().stream().mapToDouble(Double::doubleValue).sum();
            double growth = forecast.getGrowthRate() == null ? 0 : forecast.getGrowthRate();
            boolean down = growth < 0;

            Insight insight = new Insight();
            insight.setCategory("sales");
            insight.setType(down ? "warning" : "success");
            insight.setTitle(down ? "销售预测下行" : "销售预测上行");
            String desc = String.format("按%s，%s 至 %s 销售额预计合计 $%s，较最近3个月均值 $%s %s %.1f%%。",
                    forecast.getMethod() == null ? "规则模型" : forecast.getMethod(),
                    forecast.getMonths().get(0),
                    forecast.getMonths().get(forecast.getMonths().size() - 1),
                    String.format("%,.0f", forecastSum),
                    String.format("%,.0f", last3Avg),
                    down ? "下降" : "增长",
                    Math.abs(growth));
            // 补齐 60-90 字：最近两个月订单中断时点明事实
            double lastMonth = monthlySales.getOrDefault(current.minusMonths(1).format(MONTH_FMT), 0.0);
            double monthBefore = monthlySales.getOrDefault(current.minusMonths(2).format(MONTH_FMT), 0.0);
            if (lastMonth <= 0 && monthBefore <= 0) {
                desc += "最近两个月无新增有效订单，趋势持续性存疑。";
            } else {
                desc += String.format("样本覆盖 %d 个月订单数据。", forecast.getSampleMonths());
            }
            insight.setDescription(desc);
            insight.setSuggestion(down
                    ? "建议：1. 复核近两月订单流失原因；2. 对 TOP 客户启动回访；3. 制定下月促销与备货计划。"
                    : "建议：1. 锁定高增长品类备货；2. 提前排产防缺货；3. 维护核心客户复购节奏。");
            list.add(insight);
        }

        // 2. 客户流失
        if (!customerRisk.isEmpty()) {
            CustomerRisk top = customerRisk.get(0);
            CustomerRisk longest = customerRisk.stream()
                    .max(Comparator.comparingInt(CustomerRisk::getDaysSince)).orElse(top);
            double total = customerRisk.stream()
                    .mapToDouble(c -> c.getTotalSalesUsd() == null ? 0.0 : c.getTotalSalesUsd().doubleValue())
                    .sum();
            Insight insight = new Insight();
            insight.setCategory("customer");
            insight.setType("warning");
            insight.setTitle("客户流失风险预警");
            insight.setDescription(String.format(
                    "%d 家客户已超过 90 天未下单，最久客户已 %d 天无订单、历史 %d 单 $%s，风险客户历史销售额合计 $%s。",
                    customerRisk.size(), longest.getDaysSince(), longest.getOrderCount(),
                    String.format("%,.0f", longest.getTotalSalesUsd() == null ? 0 : longest.getTotalSalesUsd().doubleValue()),
                    String.format("%,.0f", total)));
            insight.setSuggestion("建议：1. 本周内逐一电话回访风险客户；2. 核查断单原因与竞品报价；3. 对高价值客户制定复购优惠。");
            list.add(insight);
        }

        // 3. 物流异常
        if (!alerts.isEmpty()) {
            int inTransit = countInTransitShipments();
            String first = alertBrief(alerts.get(0));
            String second = alerts.size() > 1 ? "；" + alertBrief(alerts.get(1)) : "";
            Insight insight = new Insight();
            insight.setCategory("logistics");
            insight.setType("warning");
            insight.setTitle("在途物流异常预警");
            insight.setDescription(String.format("%d 票在途或清关发货单中 %d 票异常：%s%s，需尽快核实。",
                    inTransit, alerts.size(), first, second));
            insight.setSuggestion("建议：1. 逐票联系承运商确认位置；2. 同步客户最新 ETA；3. 评估货损与索赔方案。");
            list.add(insight);
        }

        // 4. 库存
        List<RestockSuggestion> restock = allRestock.stream()
                .filter(r -> r.getDaysOfSupply() != null && r.getDaysOfSupply() < STOCK_MEDIUM_DAYS)
                .limit(10)
                .toList();
        if (!restock.isEmpty()) {
            RestockSuggestion top = restock.get(0);
            Insight insight = new Insight();
            insight.setCategory("stock");
            insight.setType("warning");
            insight.setTitle("库存补货预警");
            insight.setDescription(String.format("共有 %d 个产品仓库组合可售天数低于 30 天，最紧缺为 %s（%s），库存 %d 件、月均销量 %.1f 件、可售 %d 天，建议补货 %d 件。",
                    restock.size(), top.getProductName(), top.getWarehouse(), top.getStock(),
                    top.getMonthlySales(), top.getDaysOfSupply(), top.getSuggestedQty()));
            insight.setSuggestion("建议：1. 优先补货高紧缺 SKU；2. 与供应商确认交期；3. 暂缓低周转品采购。");
            list.add(insight);
        } else if (!allRestock.isEmpty()) {
            RestockSuggestion minItem = allRestock.get(0);
            int minDays = minItem.getDaysOfSupply() == null ? 0 : minItem.getDaysOfSupply();
            Insight insight = new Insight();
            insight.setCategory("stock");
            insight.setType("success");
            insight.setTitle("库存周转健康");
            insight.setDescription(String.format("近 90 天有出库的 %d 个产品仓库组合可售天数最短 %d 天（%s 库存 %d 件、月均销量 %.1f 件），均高于 30 天安全线，暂无补货缺口。",
                    allRestock.size(), minDays, minItem.getWarehouse(), minItem.getStock(), minItem.getMonthlySales()));
            insight.setSuggestion("建议：1. 保持当前补货节奏；2. 关注滞销 SKU 库龄；3. 每月复核 90 天销量口径。");
            list.add(insight);
        }

        // 5. 订单履约概况
        Map<String, Object> agg = null;
        try {
            LocalDate start = YearMonth.from(today).minusMonths(HISTORY_MONTHS - 1).atDay(1);
            agg = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) AS cnt, SUM(total_amount_usd) AS amt, "
                            + "SUM(CASE WHEN status >= 4 THEN 1 ELSE 0 END) AS done "
                            + "FROM biz_order WHERE deleted = 0 AND status BETWEEN 1 AND 5 AND order_date >= ?",
                    Date.valueOf(start));
        } catch (Exception e) {
            log.warn("业务洞察-订单履约汇总失败：{}", e.getMessage());
        }
        if (agg != null && asDouble(agg.get("cnt")) > 0) {
            int cnt = (int) asDouble(agg.get("cnt"));
            int done = (int) asDouble(agg.get("done"));
            double amount = asDouble(agg.get("amt"));
            double doneRate = round(done * 100.0 / cnt, 1);
            Insight insight = new Insight();
            insight.setCategory("sales");
            insight.setType(doneRate >= 50 ? "success" : "info");
            insight.setTitle("订单履约概况");
            insight.setDescription(String.format("近 12 个月有效订单 %d 单、累计销售额 $%s，其中已签收或完成 %d 单，占 %.1f%%，其余仍在备货或运输中。",
                    cnt, String.format("%,.0f", amount), done, doneRate));
            insight.setSuggestion("建议：1. 跟进未签收订单节点；2. 缩短备货到发货时长；3. 按月复盘履约及时率。");
            list.add(insight);
        }

        return list;
    }

    /** 在途/清关发货单总数（用于洞察文案中的分母） */
    private int countInTransitShipments() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM biz_shipment WHERE deleted = 0 AND status IN (2, 3)", Long.class);
            return count == null ? 0 : count.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private String alertBrief(LogisticsAlert alert) {
        List<String> parts = new ArrayList<>();
        if (alert.getDaysOverdue() != null && alert.getDaysOverdue() > 0) {
            parts.add("逾期 " + alert.getDaysOverdue() + " 天");
        }
        if (alert.getDaysSinceTrack() != null && alert.getDaysSinceTrack() > TRACK_STALE_DAYS) {
            parts.add("轨迹 " + alert.getDaysSinceTrack() + " 天未更新");
        }
        return alert.getShipmentNo() + " " + String.join("、", parts);
    }

    // ==================== AI 生成通道 ====================

    /**
     * 模型输入 facts：只放聚合与业务对象数据（不含员工个人信息、薪资）。
     */
    private Map<String, Object> buildFacts(RuleSnapshot snapshot) {
        BusinessInsights rule = snapshot.result;
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("统计时间", rule.getGeneratedAt());

        YearMonth current = YearMonth.from(LocalDate.now());
        List<Map<String, Object>> monthly = new ArrayList<>();
        for (int i = HISTORY_MONTHS - 1; i >= 0; i--) {
            String key = current.minusMonths(i).format(MONTH_FMT);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("月份", key);
            row.put("销售额USD", round(snapshot.monthlySales.getOrDefault(key, 0.0), 1));
            monthly.add(row);
        }
        facts.put("近12个月按月销售额USD", monthly);

        SalesForecast f = rule.getForecast() == null ? new SalesForecast() : rule.getForecast();
        Map<String, Object> forecast = new LinkedHashMap<>();
        forecast.put("预测月份", f.getMonths());
        forecast.put("预测销售额USD", f.getValues());
        forecast.put("增长率(%)", f.getGrowthRate());
        forecast.put("样本月数", f.getSampleMonths());
        forecast.put("方法", f.getMethod());
        facts.put("销售预测", forecast);

        List<Map<String, Object>> customers = new ArrayList<>();
        for (CustomerRisk c : limit(rule.getCustomerRisk(), 8)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("客户", c.getCustomerName());
            row.put("国家", c.getCountry());
            row.put("最后下单日期", c.getLastOrderDate());
            row.put("未下单天数", c.getDaysSince());
            row.put("历史订单数", c.getOrderCount());
            row.put("历史销售额USD", c.getTotalSalesUsd());
            row.put("等级", c.getLevel());
            customers.add(row);
        }
        facts.put("客户风险", customers);

        List<Map<String, Object>> restock = new ArrayList<>();
        for (RestockSuggestion r : limit(rule.getRestock(), 10)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("产品", r.getProductName());
            row.put("SKU", r.getSku());
            row.put("仓库", r.getWarehouse());
            row.put("库存件", r.getStock());
            row.put("月均销量", r.getMonthlySales());
            row.put("可售天数", r.getDaysOfSupply());
            row.put("建议补货量", r.getSuggestedQty());
            row.put("等级", r.getLevel());
            restock.add(row);
        }
        facts.put("补货建议", restock);

        List<Map<String, Object>> logistics = new ArrayList<>();
        for (LogisticsAlert a : limit(rule.getLogisticsAlerts(), 10)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("发货单", a.getShipmentNo());
            row.put("承运商", a.getCarrier());
            row.put("ETA", a.getEta());
            row.put("超期天数", a.getDaysOverdue());
            row.put("轨迹停滞天数", a.getDaysSinceTrack());
            row.put("问题", a.getIssue());
            logistics.add(row);
        }
        facts.put("物流异常", logistics);

        List<Map<String, Object>> baseline = new ArrayList<>();
        for (Insight i : rule.getInsights()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("标题", i.getTitle());
            row.put("类型", i.getType());
            row.put("类别", i.getCategory());
            row.put("描述", i.getDescription());
            row.put("建议", i.getSuggestion());
            baseline.add(row);
        }
        facts.put("规则洞察原文", baseline);
        return facts;
    }

    private <T> List<T> limit(List<T> src, int max) {
        if (src == null || src.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(src.subList(0, Math.min(max, src.size())));
    }

    /** 同步调用 DeepSeek Chat Completions（连接 5s / 读取 30s，失败由调用方回退规则） */
    private String callModel(Map<String, Object> facts) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiConfig.getModel());
        body.put("stream", false);
        body.put("max_tokens", aiConfig.getMaxTokens() == null ? 2000 : aiConfig.getMaxTokens());
        body.put("temperature", 0.3);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", objectMapper.writeValueAsString(facts)));
        body.put("messages", messages);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(aiConfig.getChatUrl()).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout(AI_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(AI_READ_TIMEOUT_MS);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IllegalStateException("模型接口返回 " + code + "：" + briefMsg(readAll(conn.getErrorStream())));
            }
            JsonNode root = objectMapper.readTree(readAll(conn.getInputStream()));
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new IllegalStateException("模型返回内容为空");
            }
            return content;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readAll(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /** 模型输出：洞察列表 + 预测解读 */
    private static class ParsedOutput {
        private List<Insight> insights;
        private String forecastComment;
    }

    /**
     * 解析并整体校验模型输出；任一条缺字段或超长都视为失败，由调用方回退规则，
     * 绝不输出「一半模型一半规则」的半成品。
     */
    private ParsedOutput parseModelOutput(String content) throws Exception {
        String text = content == null ? "" : content.trim();
        // 模型偶尔会用 ```json 包裹，先剥离
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            text = text.trim();
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("模型未返回合法 JSON");
        }

        JsonNode root = objectMapper.readTree(text.substring(start, end + 1));
        JsonNode arr = root.path("insights");
        if (!arr.isArray()) {
            throw new IllegalStateException("模型返回缺少 insights 数组");
        }
        if (arr.size() < MIN_INSIGHTS || arr.size() > MAX_INSIGHTS) {
            throw new IllegalStateException("模型返回洞察 " + arr.size() + " 条，不在 3-6 条范围内");
        }

        List<Insight> list = new ArrayList<>();
        for (JsonNode node : arr) {
            String title = textOf(node, "title");
            String type = textOf(node, "type").toLowerCase();
            String category = textOf(node, "category").toLowerCase();
            String description = textOf(node, "description");
            String suggestion = textOf(node, "suggestion");

            if (title.isEmpty() || type.isEmpty() || category.isEmpty()
                    || description.isEmpty() || suggestion.isEmpty()) {
                throw new IllegalStateException("模型返回存在缺字段的洞察条目");
            }
            if (title.length() > MAX_TITLE_LEN) {
                throw new IllegalStateException("模型返回标题超过 " + MAX_TITLE_LEN + " 字：" + title);
            }
            if (description.length() > MAX_DESC_LEN) {
                throw new IllegalStateException("模型返回描述超过 " + MAX_DESC_LEN + " 字");
            }
            if (suggestion.length() > MAX_SUGGESTION_LEN) {
                throw new IllegalStateException("模型返回建议超过 " + MAX_SUGGESTION_LEN + " 字");
            }
            if (!VALID_TYPES.contains(type)) {
                throw new IllegalStateException("模型返回未知 type：" + type);
            }
            if (!VALID_CATEGORIES.contains(category)) {
                throw new IllegalStateException("模型返回未知 category：" + category);
            }

            Insight insight = new Insight();
            insight.setTitle(title);
            insight.setType(type);
            insight.setCategory(category);
            insight.setDescription(description);
            insight.setSuggestion(suggestion);
            list.add(insight);
        }

        ParsedOutput out = new ParsedOutput();
        out.insights = list;
        out.forecastComment = cut(root.path("forecastComment").asText("").trim(), MAX_COMMENT_LEN);
        return out;
    }

    private String textOf(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    /** 读取 24h 内的 AI 结果：优先 Redis，Redis 不可用退回内存 */
    private BusinessInsights loadCached() {
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(CACHE_KEY);
                if (json != null && !json.isBlank()) {
                    BusinessInsights cached = objectMapper.readValue(json, BusinessInsights.class);
                    if (cached != null && "model".equals(cached.getSource())) {
                        return cached;
                    }
                }
            } catch (Exception e) {
                log.warn("读取业务洞察缓存失败：{}", e.getMessage());
            }
        }
        BusinessInsights memory = memoryCache;
        if (memory != null && System.currentTimeMillis() - memoryCacheAt < CACHE_TTL_MS) {
            return memory;
        }
        return null;
    }

    /** 写入完整响应：内存兜底 + Redis 24h */
    private void store(BusinessInsights result) {
        memoryCache = result;
        memoryCacheAt = System.currentTimeMillis();
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(result),
                        Duration.ofHours(CACHE_TTL_HOURS));
            } catch (Exception e) {
                log.warn("写入业务洞察缓存失败：{}", e.getMessage());
            }
        }
    }

    private String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }

    private String brief(Throwable e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return msg.length() > 160 ? msg.substring(0, 160) : msg;
    }

    private String briefMsg(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.replaceAll("\\s+", " ").trim();
        return t.length() > 160 ? t.substring(0, 160) : t;
    }

    // ==================== 类型转换 ====================

    private static double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime().toLocalDate();
        }
        return null;
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime();
        }
        return null;
    }

    private static double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
