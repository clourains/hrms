package com.hrms.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.config.AiConfig;
import com.hrms.dto.AnalyticsDTO.AiInsight;
import com.hrms.dto.AnalyticsDTO.AttendanceAnalysis;
import com.hrms.dto.AnalyticsDTO.DashboardStats;
import com.hrms.dto.AnalyticsDTO.EmployeeStructure;
import com.hrms.dto.AnalyticsDTO.PerformanceAnalysis;
import com.hrms.dto.AnalyticsDTO.SalaryAnalysis;
import com.hrms.dto.AnalyticsDTO.TalentPrediction;
import com.hrms.service.AiInsightService;
import com.hrms.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 智能洞察实现
 *
 * 设计要点：
 * - 页面默认展示「最近一次 AI 分析结果」：有落库缓存就直接回放（含生成时间与新鲜度），
 *   没有缓存时自动生成一次，保证用户进来看到的始终是模型结论而不是规则推算；
 * - 结论按天过期：24 小时内视为最新，超过 24 小时在页面显著备注「时间较久」，超过 72 小时给出更强提示；
 *   过期只提示、不自动重算，避免每次进页面都产生费用，重新生成由用户点「AI 深度分析」触发；
 * - 结果同时写入 Redis（7 天）与内存，服务重启不丢；超过 7 天直接丢弃重算，避免展示严重失真的结论；
 * - 模型超时/断网/未配置 key 时：有旧结果优先展示旧结果并说明原因，确实没有才回落规则通道并明确标注。
 */
@Slf4j
@Service
public class AiInsightServiceImpl implements AiInsightService {

    /** 结论新鲜期：24 小时内视为最新。HR 人力数据按日更新（入离职/在职数当天就会变），日更节奏足够 */
    private static final long FRESH_MS = 24 * 60 * 60 * 1000L;
    /** 明显滞后：超过 3 天给出更强提示，此时入离职、招聘缺口大概率已经改变 */
    private static final long AGING_MS = 72 * 60 * 60 * 1000L;
    /** 缓存保留上限：超过 7 天直接丢弃重算，宁可重新生成也不展示失真结论 */
    private static final long DISCARD_MS = 7 * 24 * 60 * 60 * 1000L;
    /** 结果落库的 Redis key（单条最新结果，不按用户区分：同一份公司数据所有管理员看到的一致） */
    private static final String CACHE_KEY = "hrms:analytics:ai-insight:latest";
    /** Redis 侧保留时长，比 DISCARD_MS 略长，保证边界上仍能读到并判断为过期 */
    private static final long REDIS_TTL_HOURS = 8 * 24L;
    /** 送给模型的输出上限，控制费用 */
    private static final int MAX_TOKENS = 1200;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> VALID_TYPES = Set.of("warning", "success", "info");
    /** 依据类别白名单：模型只允许从中选，后端据此回填真实依据，杜绝编造数值 */
    private static final Set<String> VALID_CATEGORIES =
            Set.of("turnover", "recruit", "performance", "salary", "attendance", "structure");

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是资深 HR 与经营数据分析师，负责向管理层提交精简的智能洞察。",
            "输入是一份来自真实数据库的统计事实（JSON），其中 ruleEngineBaseline 是规则引擎的初步判断，可参考但不要照抄。",
            "输出要求：",
            "1. 只输出 JSON，不要 markdown 代码块，不要任何解释性文字；",
            "2. 结构：{\"trendComment\":\"...\",\"insights\":[{\"title\":\"...\",\"type\":\"warning|success|info\",\"category\":\"...\",\"description\":\"...\",\"suggestion\":\"...\"}]}",
            "3. 共 4-6 条，至少包含 1 条 type=warning，按重要程度从高到低排序；",
            "4. title 不超过 14 个汉字；description 控制在 60-90 字，必须引用事实中的具体数字；",
            "5. suggestion 写成「建议：1. xxx；2. xxx；3. xxx」的形式，不超过 80 字；",
            "6. category 只能取以下之一：turnover、recruit、performance、salary、attendance、structure，用于系统回填真实依据，严禁自创；",
            "7. 严禁编造事实中不存在的数字，某项数据缺失时直接跳过该维度；",
            "8. trendComment 是对未来 12 个月人员趋势的整体解读，100-140 字：先点明趋势方向，",
            "   再指出是什么在驱动（入离职、在招缺口），最后给一句管理层该做什么，不要堆砌数字；",
            "9. 引用部门缺口时，只能使用「在招缺口(按部门)」里的数值，不要拿需求单数、现有人数代替。");

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AiConfig aiConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Redis 未部署时（开发环境常见）退回内存缓存，不影响接口可用性 */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /** 生成串行化锁：避免多个管理员同时进页面时并发调用模型、重复计费 */
    private final Object generateLock = new Object();

    /** 内存兜底缓存（Redis 不可用时使用） */
    private volatile Map<String, Object> memoryCache;

    @Override
    public Map<String, Object> getInsights(boolean generate) {
        if (!generate) {
            Map<String, Object> stored = loadStored();
            if (isUsable(stored)) {
                return present(stored, true);
            }
            // 没有缓存（首次访问或缓存超过 7 天）：自动生成一次，页面始终展示模型结论而不是规则推算
            synchronized (generateLock) {
                stored = loadStored();
                if (isUsable(stored)) {
                    return present(stored, true);
                }
                return generateOnce();
            }
        }
        // 用户点「AI 深度分析」：强制重算，忽略缓存
        synchronized (generateLock) {
            return generateOnce();
        }
    }

    /** 真正调用一次模型；失败时优先退回上一次结果，确实没有再退回规则通道 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> generateOnce() {
        List<AiInsight> rule = safeRuleInsights();

        if (!Boolean.TRUE.equals(aiConfig.getEnabled())
                || aiConfig.getApiKey() == null || aiConfig.getApiKey().isBlank()) {
            return degrade(rule, "智能分析服务未启用（缺少 API Key 配置）");
        }

        long start = System.currentTimeMillis();
        try {
            Map<String, Object> facts = buildFacts(rule);
            String content = callModel(facts);
            Map<String, Object> parsed = parseModelOutput(content);
            List<AiInsight> list = (List<AiInsight>) parsed.get("list");
            if (list == null || list.isEmpty()) {
                throw new IllegalStateException("模型未返回有效洞察条目");
            }
            String trendComment = (String) parsed.get("trendComment");
            if (trendComment == null || trendComment.isBlank()) {
                trendComment = buildTrendSummary();
            }
            long elapsed = System.currentTimeMillis() - start;

            Map<String, Object> res = wrap("model", list, aiConfig.getModel(),
                    elapsed, false, null, trendComment);
            store(res);
            log.info("AI 洞察生成成功：model={}, items={}, 耗时={}ms", aiConfig.getModel(), list.size(), elapsed);
            return present(res, false);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            // 具体异常只写日志：界面面向业务人员，不展示超时、连接等底层信息
            log.warn("AI 洞察生成失败：{}", brief(e));
            return degrade(rule, "智能分析服务调用失败");
        }
    }

    /** 降级：有可用的历史结果就继续展示历史结果，否则才退回规则推算 */
    private Map<String, Object> degrade(List<AiInsight> rule, String reason) {
        Map<String, Object> stored = loadStored();
        if (isUsable(stored)) {
            Map<String, Object> res = present(stored, true);
            res.put("fallbackReason", reason + "，当前展示上一次分析结果");
            return res;
        }
        Map<String, Object> res = wrap("rule", rule, null, 0L, false,
                reason + "，以下为规则推算", buildTrendSummary());
        res.put("ageHours", 0L);
        res.put("expireLevel", "fresh");
        res.put("expireNote", null);
        return res;
    }

    // ==================== 结果留存与新鲜度 ====================

    /** 读取最近一次结果：优先 Redis（重启不丢），Redis 不可用时退回内存 */
    private Map<String, Object> loadStored() {
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(CACHE_KEY);
                if (json != null && !json.isBlank()) {
                    return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                }
            } catch (Exception e) {
                log.warn("读取洞察缓存失败，退回内存缓存：{}", e.getMessage());
            }
        }
        return memoryCache;
    }

    private void store(Map<String, Object> res) {
        memoryCache = res;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(res),
                        Duration.ofHours(REDIS_TTL_HOURS));
            } catch (Exception e) {
                log.warn("写入洞察缓存失败：{}", e.getMessage());
            }
        }
    }

    /** 超过 7 天的结果视为不可用，强制重算 */
    private boolean isUsable(Map<String, Object> stored) {
        if (stored == null) {
            return false;
        }
        long at = toLong(stored.get("generatedAtMs"));
        return at > 0L && System.currentTimeMillis() - at < DISCARD_MS;
    }

    /** 回放结果时补上生成时间距今多久、是否过期，前端据此给备注 */
    private Map<String, Object> present(Map<String, Object> stored, boolean cached) {
        Map<String, Object> res = new LinkedHashMap<>(stored);
        long at = toLong(stored.get("generatedAtMs"));
        long ageMs = at > 0L ? Math.max(0L, System.currentTimeMillis() - at) : 0L;
        long hours = ageMs / (60L * 60L * 1000L);
        String level;
        if (ageMs < FRESH_MS) {
            level = "fresh";
        } else if (ageMs < AGING_MS) {
            level = "aging";
        } else {
            level = "stale";
        }
        res.put("cached", cached);
        res.put("ageHours", hours);
        res.put("expireLevel", level);
        res.put("expireNote", expireNote(level, hours));
        if (cached) {
            res.put("elapsedMs", 0L);
        }
        return res;
    }

    private String expireNote(String level, long hours) {
        if ("aging".equals(level)) {
            return "该结论生成于 " + hours + " 小时前（超过 24 小时），此后的人员异动与在招进展未纳入本次分析，"
                    + "建议重新生成后再引用。";
        }
        if ("stale".equals(level)) {
            long days = Math.max(1L, hours / 24L);
            return "该结论生成于 " + days + " 天前，与当前数据可能存在较大偏差，建议重新生成后再引用。";
        }
        return null;
    }

    // ==================== 规则通道 ====================

    private List<AiInsight> safeRuleInsights() {
        try {
            List<AiInsight> list = analyticsService.getAiInsights();
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("规则洞察计算失败：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==================== 事实汇总 ====================

    private Map<String, Object> buildFacts(List<AiInsight> ruleInsights) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("统计时间", LocalDateTime.now().format(TS));

        try {
            DashboardStats s = analyticsService.getDashboardStats();
            if (s != null) {
                Map<String, Object> basic = new LinkedHashMap<>();
                basic.put("员工总数", s.getTotalEmployees());
                basic.put("在职员工", s.getActiveEmployees());
                basic.put("本月入职", s.getNewEmployees());
                basic.put("本月离职", s.getResignedEmployees());
                basic.put("部门数", s.getDeptCount());
                basic.put("岗位数", s.getPositionCount());
                basic.put("本月出勤率(%)", s.getAttendanceRate());
                basic.put("平均绩效分", s.getAvgPerformance());
                basic.put("本月人均薪资(元)", s.getAvgSalary());
                facts.put("公司概览", basic);
            }
        } catch (Exception e) {
            log.warn("取仪表盘统计失败：{}", e.getMessage());
        }

        try {
            EmployeeStructure es = analyticsService.getEmployeeStructure();
            if (es != null) {
                Map<String, Object> structure = new LinkedHashMap<>();
                structure.put("部门分布", top(es.getDepartmentDistribution(), 8));
                structure.put("学历分布", top(es.getEducationDistribution(), 6));
                structure.put("年龄分布", top(es.getAgeDistribution(), 6));
                structure.put("工龄分布", top(es.getWorkYearsDistribution(), 6));
                facts.put("人员结构", structure);
            }
        } catch (Exception e) {
            log.warn("取人员结构失败：{}", e.getMessage());
        }

        try {
            SalaryAnalysis sa = analyticsService.getSalaryAnalysis(null);
            if (sa != null) {
                Map<String, Object> salary = new LinkedHashMap<>();
                salary.put("薪资总额(元)", sa.getTotalSalary());
                salary.put("平均薪资(元)", sa.getAvgSalary());
                salary.put("薪资中位数(元)", sa.getMedianSalary());
                salary.put("最高薪资(元)", sa.getMaxSalary());
                salary.put("最低薪资(元)", sa.getMinSalary());
                salary.put("部门薪资对比", top(sa.getDeptSalaryComparison(), 8));
                facts.put("薪资情况", salary);
            }
        } catch (Exception e) {
            log.warn("取薪资分析失败：{}", e.getMessage());
        }

        try {
            PerformanceAnalysis pa = analyticsService.getPerformanceAnalysis(null);
            if (pa != null) {
                Map<String, Object> perf = new LinkedHashMap<>();
                perf.put("平均总分", pa.getAvgTotalScore());
                perf.put("绩效等级分布", top(pa.getGradeDistribution(), 8));
                perf.put("部门绩效对比", top(pa.getDeptPerformance(), 8));
                facts.put("绩效情况", perf);
            }
        } catch (Exception e) {
            log.warn("取绩效分析失败：{}", e.getMessage());
        }

        try {
            AttendanceAnalysis aa = analyticsService.getAttendanceAnalysis(null);
            if (aa != null) {
                Map<String, Object> att = new LinkedHashMap<>();
                att.put("整体出勤率(%)", aa.getOverallAttendanceRate());
                att.put("迟到率(%)", aa.getLateRate());
                att.put("早退率(%)", aa.getEarlyLeaveRate());
                att.put("缺勤率(%)", aa.getAbsentRate());
                att.put("平均工时", aa.getAvgWorkHours());
                facts.put("考勤情况", att);
            }
        } catch (Exception e) {
            log.warn("取考勤分析失败：{}", e.getMessage());
        }

        try {
            TalentPrediction tp = analyticsService.getTalentPrediction();
            if (tp != null) {
                Map<String, Object> talent = new LinkedHashMap<>();
                talent.put("预测离职率(%)", tp.getPredictedAttritionRate());
                talent.put("高风险员工数", tp.getHighRiskEmployees() == null ? 0 : tp.getHighRiskEmployees().size());
                talent.put("晋升候选人数", tp.getPromotionCandidates() == null ? 0 : tp.getPromotionCandidates().size());
                // 未来 12 个月人数曲线（按近 12 个月真实入离职均值外推）与在招缺口，
                // 供模型撰写 trendComment，避免它凭空猜测趋势。
                // 缺口只保留「部门 + 缺口人数」两个字段：早前把 openRequisitions（需求单数）
                // 一起塞进来，模型会把它误当成缺口人数，故精简掉易混字段。
                talent.put("未来12个月人数预测", top(tp.getFutureHeadcount(), 12));
                talent.put("在招缺口(按部门)", openGapList(tp.getRecruitmentDemand()));
                facts.put("人才预测", talent);
            }
        } catch (Exception e) {
            log.warn("取人才预测失败：{}", e.getMessage());
        }

        try {
            facts.put("近6个月入离职", top(analyticsService.getTurnoverTrend(6), 6));
        } catch (Exception e) {
            log.warn("取入离职趋势失败：{}", e.getMessage());
        }

        if (ruleInsights != null && !ruleInsights.isEmpty()) {
            List<Map<String, Object>> baseline = new ArrayList<>();
            for (AiInsight i : ruleInsights) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("title", i.getTitle());
                m.put("finding", i.getDescription());
                baseline.add(m);
            }
            facts.put("ruleEngineBaseline", baseline);
        }

        return facts;
    }

    private List<Map<String, Object>> top(List<Map<String, Object>> src, int limit) {
        if (src == null || src.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(src.subList(0, Math.min(limit, src.size())));
    }

    /**
     * 只抽出有缺口的部门，字段精简为「部门 / 在招缺口人数」，
     * 避免模型把需求单数、现有人数等相邻字段误读成缺口。
     */
    private List<Map<String, Object>> openGapList(List<Map<String, Object>> demand) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (demand == null) {
            return out;
        }
        for (Map<String, Object> r : demand) {
            int gap = toInt(r.get("predictedDemand"));
            if (gap <= 0) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("部门", r.get("position"));
            item.put("在招缺口人数", gap);
            out.add(item);
        }
        return out;
    }

    // ==================== 模型调用 ====================

    private String callModel(Map<String, Object> facts) throws Exception {
        String factsJson = objectMapper.writeValueAsString(facts);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiConfig.getModel());
        body.put("stream", false);
        body.put("max_tokens", MAX_TOKENS);
        body.put("temperature", 0.3);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", factsJson));
        body.put("messages", messages);

        HttpURLConnection conn = null;
        try {
            URL url = new URL(aiConfig.getChatUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                String err = readAll(conn.getErrorStream());
                throw new IllegalStateException("模型接口返回 " + code + "：" + briefMsg(err));
            }

            String resp = readAll(conn.getInputStream());
            JsonNode root = objectMapper.readTree(resp);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("模型返回内容为空");
            }
            return content.asText();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readAll(java.io.InputStream in) throws Exception {
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

    // ==================== 结果解析 ====================

    /**
     * 解析模型输出，同时取出洞察列表与趋势解读。
     * 返回 map：list -> List&lt;AiInsight&gt;，trendComment -> String（可能为空）
     */
    private Map<String, Object> parseModelOutput(String content) throws Exception {
        String text = content == null ? "" : content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("模型未返回合法 JSON");
        }

        JsonNode root = objectMapper.readTree(text.substring(start, end + 1));
        String trendComment = cut(root.path("trendComment").asText("").trim(), 400);

        JsonNode arr = root.path("insights");
        if (!arr.isArray()) {
            arr = root.path("data");
        }
        if (!arr.isArray()) {
            throw new IllegalStateException("模型返回缺少 insights 数组");
        }

        List<AiInsight> list = new ArrayList<>();
        for (JsonNode node : arr) {
            String title = node.path("title").asText("").trim();
            if (title.isEmpty()) {
                continue;
            }
            AiInsight insight = new AiInsight();
            insight.setTitle(cut(title, 30));
            String type = node.path("type").asText("info").trim();
            insight.setType(VALID_TYPES.contains(type) ? type : "info");
            insight.setDescription(cut(node.path("description").asText("").trim(), 300));
            insight.setSuggestion(cut(node.path("suggestion").asText("").trim(), 300));
            // 类别只认白名单，模型自创的类别一律置空，该条将不带依据块（宁缺勿造）
            String category = node.path("category").asText("").trim().toLowerCase();
            insight.setCategory(VALID_CATEGORIES.contains(category) ? category : null);
            double confidence = node.path("confidence").asDouble(0.8);
            insight.setConfidence(Math.max(0.5, Math.min(0.98, confidence)));
            list.add(insight);
        }

        if (list.isEmpty()) {
            throw new IllegalStateException("模型返回的洞察为空");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", list.size() > 6 ? new ArrayList<>(list.subList(0, 6)) : list);
        out.put("trendComment", trendComment);
        return out;
    }

    // ==================== 依据组装（全部走真实库表，不经过模型） ====================

    /**
     * 给每条洞察补上「依据明细 / 计算口径 / 可执行动作」。
     *
     * 红线：数值一律由后端按 category 查库组装，模型只负责写文字。
     * 模型给出的 category 不在白名单时，该条不带依据块——宁缺勿造。
     */
    private void enrich(List<AiInsight> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<String, Object> memo = new LinkedHashMap<>();
        for (AiInsight ins : list) {
            if (ins == null) {
                continue;
            }
            if (ins.getActions() == null || ins.getActions().isEmpty()) {
                ins.setActions(splitActions(ins.getSuggestion()));
            }
            String cat = ins.getCategory();
            boolean known = cat != null && VALID_CATEGORIES.contains(cat);
            // 目标页按类别固定映射，模型不参与——避免它编造链接和筛选参数
            if (known && (ins.getTargets() == null || ins.getTargets().isEmpty())) {
                ins.setTargets(defaultTargets(cat));
            }
            if (ins.getEvidence() != null) {
                continue;   // 规则通道已自带依据，不重复组装
            }
            if (!known) {
                ins.setEvidence(new ArrayList<>());
                continue;
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            try {
                ins.setBasis(buildEvidence(cat, rows, memo));
            } catch (Exception e) {
                log.warn("组装洞察依据失败 category={}：{}", cat, e.getMessage());
            }
            ins.setEvidence(rows);
        }
    }

    private String buildEvidence(String cat, List<Map<String, Object>> out, Map<String, Object> memo) {
        switch (cat) {
            case "turnover": {
                TalentPrediction tp = cached(memo, "talent", analyticsService::getTalentPrediction);
                DashboardStats ds = cached(memo, "stats", analyticsService::getDashboardStats);
                if (tp != null) {
                    out.add(row("离职率（近 6 个月口径）", pct(tp.getPredictedAttritionRate()), "sys_employee.leave_date"));
                    out.add(row("高风险员工", size(tp.getHighRiskEmployees()), "工龄 + 薪资 + 绩效综合评分"));
                    out.add(row("晋升候选人", size(tp.getPromotionCandidates()), "综合潜力评分"));
                }
                if (ds != null) {
                    out.add(row("当前在职", cnt(ds.getActiveEmployees()), "sys_employee.status = 1"));
                    out.add(row("本月离职", cnt(ds.getResignedEmployees()), "sys_employee.status = 0"));
                }
                return "离职率 = 近 6 个月离职人数 ÷ 当前在职人数；与「该部门离职人数 ÷ 该部门总人数」"
                        + "的部门口径不同，两个数不能直接比较。";
            }
            case "recruit": {
                TalentPrediction tp = cached(memo, "talent", analyticsService::getTalentPrediction);
                if (tp != null && tp.getRecruitmentDemand() != null) {
                    for (Map<String, Object> r : tp.getRecruitmentDemand()) {
                        int gap = toInt(r.get("predictedDemand"));
                        if (gap > 0) {
                            out.add(row(String.valueOf(r.get("position")), gap + " 人", "hr_recruit_requisition（status = 1）"));
                        }
                    }
                }
                return "在招缺口 = 需求单 headcount − 已招 hiredCount，只统计「招聘中」的需求单；"
                        + "需求单的张数不等于缺口人数。";
            }
            case "performance": {
                PerformanceAnalysis pa = cached(memo, "perf", () -> analyticsService.getPerformanceAnalysis(null));
                if (pa != null) {
                    out.add(row("平均总分", num(pa.getAvgTotalScore()), "sys_performance.total_score"));
                    out.add(row("S 级记录", grade(pa.getGradeDistribution(), "S"), "sys_performance.grade"));
                    out.add(row("A 级记录", grade(pa.getGradeDistribution(), "A"), "sys_performance.grade"));
                }
                return "按考核记录统计，S/A 级计为高绩效；同一员工多次考核会重复计入，所以是「人次」而非「人数」。";
            }
            case "salary": {
                SalaryAnalysis sa = cached(memo, "salary", () -> analyticsService.getSalaryAnalysis(null));
                if (sa != null) {
                    out.add(row("平均薪资", money(sa.getAvgSalary()), "sys_employee.salary"));
                    out.add(row("薪资中位数", money(sa.getMedianSalary()), "sys_employee.salary"));
                    out.add(row("最高 / 最低", money(sa.getMaxSalary()) + " / " + money(sa.getMinSalary()), "sys_employee.salary"));
                }
                return "「薪资偏低」判定为个人薪资 < 平均薪资 × 80%，按在职员工口径统计。";
            }
            case "attendance": {
                AttendanceAnalysis aa = cached(memo, "attendance", () -> analyticsService.getAttendanceAnalysis(null));
                if (aa != null) {
                    out.add(row("整体出勤率", pct(aa.getOverallAttendanceRate()), "sys_attendance"));
                    out.add(row("迟到率", pct(aa.getLateRate()), "sys_attendance.status = 2"));
                    out.add(row("早退率", pct(aa.getEarlyLeaveRate()), "sys_attendance.status = 3"));
                    out.add(row("缺勤率", pct(aa.getAbsentRate()), "sys_attendance.status = 4"));
                }
                return "异常率 = 迟到 + 早退 + 缺勤记录数 ÷ 考勤记录总数；分母是全部考勤记录，不是员工人数。";
            }
            case "structure": {
                EmployeeStructure es = cached(memo, "structure", analyticsService::getEmployeeStructure);
                DashboardStats ds = cached(memo, "stats", analyticsService::getDashboardStats);
                if (ds != null) {
                    out.add(row("当前在职", cnt(ds.getActiveEmployees()), "sys_employee.status = 1"));
                }
                if (es != null) {
                    out.add(row("本科及以上", ratio(es.getEducationDistribution(), Set.of("本科", "硕士", "博士")), "sys_employee.education"));
                    out.add(row("主要年龄段", topName(es.getAgeDistribution()), "sys_employee.birthday"));
                    out.add(row("部门数", (es.getDepartmentDistribution() == null ? 0 : es.getDepartmentDistribution().size()) + " 个", "sys_department"));
                }
                return "占比按在职员工统计，「本科及以上」含本科、硕士、博士；未填学历的员工不计入，分母可能小于在职人数。";
            }
            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(Map<String, Object> memo, String key, java.util.function.Supplier<T> loader) {
        if (memo.containsKey(key)) {
            return (T) memo.get(key);
        }
        T value;
        try {
            value = loader.get();
        } catch (Exception e) {
            log.warn("取数失败 {}：{}", key, e.getMessage());
            value = null;
        }
        memo.put(key, value);
        return value;
    }

    /**
     * 模型通道的目标页：按类别给固定入口，不带筛选参数
     * （筛选需要真实 id，只有规则通道掌握）。
     */
    private List<Map<String, Object>> defaultTargets(String cat) {
        List<Map<String, Object>> targets = new ArrayList<>();
        String label;
        String path;
        switch (cat) {
            case "turnover":
            case "structure":
                label = "查看员工列表";
                path = "/employee";
                break;
            case "recruit":
                label = "查看招聘需求";
                path = "/recruit/requisition";
                break;
            case "performance":
                label = "查看绩效记录";
                path = "/performance";
                break;
            case "salary":
                label = "查看薪资明细";
                path = "/salary";
                break;
            case "attendance":
                label = "查看考勤记录";
                path = "/attendance";
                break;
            default:
                return targets;
        }
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("label", label);
        target.put("path", path);
        targets.add(target);
        return targets;
    }

    private Map<String, Object> row(String label, String value, String source) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("source", source);
        return m;
    }

    /** 把「建议：1. xxx；2. xxx；3. xxx」拆成可逐条勾选的动作 */
    private List<String> splitActions(String suggestion) {
        List<String> list = new ArrayList<>();
        if (suggestion == null || suggestion.isBlank()) {
            return list;
        }
        String s = suggestion.trim();
        if (s.startsWith("建议：") || s.startsWith("建议:")) {
            s = s.substring(3);
        }
        for (String part : s.split("[；;\\n]")) {
            String t = part.trim()
                    .replaceFirst("^[0-9]+\\s*[.、)）]\\s*", "")
                    .replaceFirst("^[①②③④⑤⑥⑦]\\s*", "")
                    .replaceAll("[。.]$", "");
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        return list;
    }

    private String pct(Object v) {
        return v == null ? "—" : String.format("%.1f%%", toDouble(v));
    }

    private String money(Object v) {
        return v == null ? "—" : String.format("%,.0f 元", toDouble(v));
    }

    private String num(Object v) {
        return v == null ? "—" : String.format("%.1f", toDouble(v));
    }

    private String cnt(Object v) {
        return v == null ? "—" : toInt(v) + " 人";
    }

    private String size(List<?> l) {
        return (l == null ? 0 : l.size()) + " 人";
    }

    private String grade(List<Map<String, Object>> dist, String name) {
        if (dist != null) {
            for (Map<String, Object> m : dist) {
                if (name.equals(String.valueOf(m.get("name")))) {
                    return toLong(m.get("value")) + " 人次";
                }
            }
        }
        return "0 人次";
    }

    private String ratio(List<Map<String, Object>> dist, Set<String> keys) {
        if (dist == null || dist.isEmpty()) {
            return "—";
        }
        long total = 0L;
        long hit = 0L;
        for (Map<String, Object> m : dist) {
            long v = toLong(m.get("value"));
            total += v;
            if (keys.contains(String.valueOf(m.get("name")))) {
                hit += v;
            }
        }
        return total == 0 ? "—" : String.format("%.1f%%（%d / %d 人）", hit * 100.0 / total, hit, total);
    }

    private String topName(List<Map<String, Object>> dist) {
        if (dist == null || dist.isEmpty()) {
            return "—";
        }
        Map<String, Object> best = null;
        long bestValue = -1L;
        for (Map<String, Object> m : dist) {
            long v = toLong(m.get("value"));
            if (v > bestValue) {
                bestValue = v;
                best = m;
            }
        }
        return best == null ? "—" : best.get("name") + "（" + bestValue + " 人）";
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0d;
        }
    }

    private long toLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }

    // ==================== 组装响应 ====================

    private Map<String, Object> wrap(String source, List<AiInsight> list, String model,
                                     long elapsedMs, boolean cached, String fallbackReason,
                                     String trendComment) {
        enrich(list);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("list", list);
        res.put("source", source);
        res.put("model", model);
        res.put("generatedAt", LocalDateTime.now().format(TS));
        res.put("generatedAtMs", System.currentTimeMillis());
        res.put("elapsedMs", elapsedMs);
        res.put("cached", cached);
        res.put("fallbackReason", fallbackReason);
        res.put("trendComment", trendComment);
        return res;
    }

    // ==================== 趋势解读（规则版，0 成本） ====================

    /**
     * 规则通道的趋势解读：只用已经算好的真实数字拼装，不联网、不计费。
     * 模型通道成功时会用模型撰写的版本覆盖它。
     */
    private String buildTrendSummary() {
        try {
            TalentPrediction tp = analyticsService.getTalentPrediction();
            if (tp == null) {
                return null;
            }
            int now = 0;
            try {
                DashboardStats ds = analyticsService.getDashboardStats();
                if (ds != null && ds.getActiveEmployees() != null) {
                    now = ds.getActiveEmployees();
                }
            } catch (Exception ignore) {
                // 拿不到在职数就只输出趋势部分
            }

            List<Map<String, Object>> future = tp.getFutureHeadcount();
            int end = 0;
            String endMonth = null;
            if (future != null && !future.isEmpty()) {
                Map<String, Object> last = future.get(future.size() - 1);
                end = toInt(last.get("predictedCount"));
                endMonth = last.get("month") == null ? null : String.valueOf(last.get("month"));
            }

            int openGap = 0;
            List<Map<String, Object>> reqs = tp.getRecruitmentDemand();
            if (reqs != null) {
                for (Map<String, Object> r : reqs) {
                    openGap += toInt(r.get("predictedDemand"));
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("当前在职 ").append(now).append(" 人，近 6 个月离职率 ")
                    .append(tp.getPredictedAttritionRate() == null ? 0 : tp.getPredictedAttritionRate()).append("%");
            if (end > 0 && endMonth != null) {
                int diff = end - now;
                sb.append("；按近 12 个月真实入离职外推，").append(endMonth).append(" 约 ").append(end).append(" 人");
                if (diff == 0) {
                    sb.append("，规模基本持平");
                } else {
                    sb.append("，净").append(diff > 0 ? "增 " : "减 ").append(Math.abs(diff)).append(" 人");
                }
            }
            sb.append("。");
            if (openGap > 0) {
                sb.append("在招需求尚有 ").append(openGap).append(" 人缺口未补齐，若如期到岗可抵消自然流失。");
            } else {
                sb.append("当前无在招缺口，人员规模主要受自然流失影响。");
            }
            sb.append("以上取自真实入离职与招聘记录，非模型推测。");
            return sb.toString();
        } catch (Exception e) {
            log.warn("生成趋势解读失败：{}", e.getMessage());
            return null;
        }
    }

    private int toInt(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
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
        return msg.length() > 120 ? msg.substring(0, 120) : msg;
    }

    private String briefMsg(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.replaceAll("\\s+", " ").trim();
        return t.length() > 120 ? t.substring(0, 120) : t;
    }
}
