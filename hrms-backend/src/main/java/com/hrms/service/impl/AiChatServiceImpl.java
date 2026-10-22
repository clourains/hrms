package com.hrms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.config.AiConfig;
import com.hrms.entity.*;
import com.hrms.mapper.*;
import com.hrms.service.AiChatService;
import com.hrms.service.AnalyticsService;
import com.hrms.dto.AnalyticsDTO.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AiChatServiceImpl.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AiConfig aiConfig;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private SalaryMapper salaryMapper;

    @Autowired
    private PerformanceMapper performanceMapper;

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public SseEmitter chat(String message, String conversationId) {
        SseEmitter emitter = new SseEmitter(120000L);

        executor.execute(() -> {
            try {
                String systemPrompt = buildSystemPrompt();
                String userMessage = message;

                if (aiConfig.getEnabled() && !aiConfig.getApiKey().isEmpty()) {
                    callAiApi(emitter, systemPrompt, userMessage);
                } else {
                    respondFallback(emitter, userMessage);
                }
            } catch (Exception e) {
                log.error("AI chat error", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI service error: " + e.getMessage()));
                } catch (Exception ignored) {}
            } finally {
                emitter.complete();
            }
        });

        return emitter;
    }

    private void callAiApi(SseEmitter emitter, String systemPrompt, String userMessage) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(aiConfig.getChatUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiConfig.getModel());
            requestBody.put("stream", true);
            requestBody.put("max_tokens", aiConfig.getMaxTokens());
            requestBody.put("temperature", aiConfig.getTemperature());

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorMsg = "AI API returned " + responseCode;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    errorMsg += ": " + sb.toString();
                }
                emitter.send(SseEmitter.event().name("error").data(errorMsg));
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            JsonNode choices = node.path("choices");
                            if (choices.isArray() && !choices.isEmpty()) {
                                String content = choices.get(0).path("delta").path("content").asText("");
                                if (!content.isEmpty()) {
                                    emitter.send(SseEmitter.event().name("content")
                                            .data(objectMapper.writeValueAsString(Map.of("content", content))));
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Skip parse: {}", data);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("AI API call failed", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("AI API connection failed: " + e.getMessage()));
            } catch (Exception ignored) {}
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void respondFallback(SseEmitter emitter, String userMessage) throws Exception {
        String response = generateLocalResponse(userMessage);
        for (int i = 0; i < response.length(); i++) {
            int end = Math.min(i + 2, response.length());
            String chunk = response.substring(i, end);
            emitter.send(SseEmitter.event().name("content")
                    .data(objectMapper.writeValueAsString(Map.of("content", chunk))));
            i = end - 1;
            Thread.sleep(20);
        }
    }

    private String generateLocalResponse(String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("我是HRMS智能助手。当前AI模型未配置API Key，我将以内置规则为您回答。\n\n");
        sb.append("您的问题：").append(userMessage).append("\n\n");

        String lower = userMessage.toLowerCase();
        if (lower.contains("员工") || lower.contains("多少人")) {
            List<Employee> emps = employeeMapper.selectList(null);
            long active = emps.stream().filter(e -> e.getStatus() == 1).count();
            sb.append("当前员工总数：").append(emps.size()).append("人\n");
            sb.append("在职员工：").append(active).append("人\n");
        } else if (lower.contains("部门")) {
            List<Department> depts = departmentMapper.selectList(null);
            sb.append("共有部门：").append(depts.size()).append("个\n");
            for (Department d : depts) {
                sb.append("  - ").append(d.getDeptName()).append("\n");
            }
        } else if (lower.contains("薪资") || lower.contains("工资")) {
            List<Employee> emps = employeeMapper.selectList(null);
            double avg = emps.stream().filter(e -> e.getSalary() != null).mapToDouble(e -> e.getSalary().doubleValue()).average().orElse(0);
            sb.append("平均薪资：").append(String.format("%.2f", avg)).append("元\n");
        } else if (lower.contains("考勤")) {
            sb.append("您可以在考勤管理页面查看详细考勤数据。\n");
        } else if (lower.contains("绩效")) {
            sb.append("您可以在绩效管理页面查看绩效详情。\n");
        } else {
            sb.append("您可以问我关于员工、部门、薪资、考勤、绩效等方面的问题。\n");
            sb.append("配置AI API Key后，我可以提供更智能的分析和建议。\n");
        }
        return sb.toString();
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能人事管理系统(HRMS)的AI助手。以下是公司实时数据，请基于数据回答问题：\n\n");
        sb.append(buildHrContext());
        sb.append("\n请根据以上数据回答用户问题。回答要简洁、专业、有数据支撑。");
        return sb.toString();
    }

    @Override
    public String buildHrContext() {
        StringBuilder sb = new StringBuilder();

        try {
            DashboardStats stats = analyticsService.getDashboardStats();
            sb.append("【公司概况】\n");
            sb.append("员工总数: ").append(stats.getTotalEmployees()).append("人\n");
            sb.append("在职员工: ").append(stats.getActiveEmployees()).append("人\n");
            sb.append("本月入职: ").append(stats.getNewEmployees()).append("人\n");
            sb.append("离职员工: ").append(stats.getResignedEmployees()).append("人\n");
            sb.append("部门数: ").append(stats.getDeptCount()).append("个\n");
            sb.append("平均薪资: ").append(stats.getAvgSalary()).append("元\n");
            sb.append("出勤率: ").append(stats.getAttendanceRate()).append("%\n\n");

            EmployeeStructure structure = analyticsService.getEmployeeStructure();
            sb.append("【人员结构】\n");
            sb.append("性别分布: ").append(structure.getGenderDistribution()).append("\n");
            sb.append("年龄分布: ").append(structure.getAgeDistribution()).append("\n");
            sb.append("学历分布: ").append(structure.getEducationDistribution()).append("\n");
            sb.append("部门分布: ").append(structure.getDepartmentDistribution()).append("\n");
            sb.append("工龄分布: ").append(structure.getWorkYearsDistribution()).append("\n\n");

            AttendanceAnalysis attendance = analyticsService.getAttendanceAnalysis(null);
            sb.append("【考勤分析】\n");
            sb.append("出勤率: ").append(attendance.getOverallAttendanceRate()).append("%\n");
            sb.append("迟到率: ").append(attendance.getLateRate()).append("%\n");
            sb.append("早退率: ").append(attendance.getEarlyLeaveRate()).append("%\n");
            sb.append("缺勤率: ").append(attendance.getAbsentRate()).append("%\n\n");

            SalaryAnalysis salary = analyticsService.getSalaryAnalysis(null);
            sb.append("【薪资分析】\n");
            sb.append("薪资总额: ").append(salary.getTotalSalary()).append("元\n");
            sb.append("平均薪资: ").append(salary.getAvgSalary()).append("元\n");
            sb.append("薪资中位数: ").append(salary.getMedianSalary()).append("元\n");
            sb.append("薪资分布: ").append(salary.getSalaryDistribution()).append("\n\n");

            PerformanceAnalysis performance = analyticsService.getPerformanceAnalysis(null);
            sb.append("【绩效分析】\n");
            sb.append("平均绩效分: ").append(performance.getAvgTotalScore()).append("\n");
            sb.append("等级分布: ").append(performance.getGradeDistribution()).append("\n\n");

            TalentPrediction prediction = analyticsService.getTalentPrediction();
            sb.append("【人才预测】\n");
            sb.append("预测离职率: ").append(prediction.getPredictedAttritionRate()).append("%\n");
            sb.append("高风险员工数: ").append(prediction.getHighRiskEmployees().size()).append("人\n");
            sb.append("晋升候选人数: ").append(prediction.getPromotionCandidates().size()).append("人\n\n");

            List<AiInsight> insights = analyticsService.getAiInsights();
            sb.append("【AI洞察】\n");
            for (AiInsight insight : insights) {
                sb.append("- ").append(insight.getTitle()).append(": ").append(insight.getDescription()).append("\n");
            }
        } catch (Exception e) {
            log.error("Build HR context error", e);
            sb.append("数据加载异常: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }
}
