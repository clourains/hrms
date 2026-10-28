package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.AnalyticsDTO.*;
import com.hrms.dto.BusinessInsightDTO.BusinessInsights;
import com.hrms.service.AiInsightService;
import com.hrms.service.AnalyticsService;
import com.hrms.service.BusinessInsightService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能分析控制器
 */
@RestController
@RequestMapping("/analytics")
@RequireRole({"admin", "manager"})
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AiInsightService aiInsightService;

    @Autowired
    private BusinessInsightService businessInsightService;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard")
    public Result<DashboardStats> getDashboardStats() {
        return Result.success(analyticsService.getDashboardStats());
    }

    /**
     * 获取人员结构分析
     */
    @GetMapping("/employee-structure")
    public Result<EmployeeStructure> getEmployeeStructure() {
        return Result.success(analyticsService.getEmployeeStructure());
    }

    /**
     * 获取考勤分析
     */
    @GetMapping("/attendance")
    public Result<AttendanceAnalysis> getAttendanceAnalysis(
            @RequestParam(required = false) String month) {
        return Result.success(analyticsService.getAttendanceAnalysis(month));
    }

    /**
     * 获取薪资分析
     */
    @GetMapping("/salary")
    public Result<SalaryAnalysis> getSalaryAnalysis(
            @RequestParam(required = false) String month) {
        return Result.success(analyticsService.getSalaryAnalysis(month));
    }

    /**
     * 获取绩效分析
     */
    @GetMapping("/performance")
    public Result<PerformanceAnalysis> getPerformanceAnalysis(
            @RequestParam(required = false) String period) {
        return Result.success(analyticsService.getPerformanceAnalysis(period));
    }

    /**
     * 获取人才预测
     */
    @GetMapping("/talent-prediction")
    public Result<TalentPrediction> getTalentPrediction() {
        return Result.success(analyticsService.getTalentPrediction());
    }

    /**
     * 智能洞察
     *
     * generate=false（默认）：规则计算，本地生成，不联网、不产生费用；
     * generate=true：把统计数据交给大模型生成洞察（用户主动触发，结果缓存 10 分钟），
     * 模型不可用时自动回落规则结果，并在 fallbackReason 中说明原因。
     */
    @GetMapping("/ai-insights")
    public Result<Map<String, Object>> getAiInsights(
            @RequestParam(defaultValue = "false") boolean generate) {
        return Result.success(aiInsightService.getInsights(generate));
    }

    /**
     * 业务智能洞察：销售预测、客户流失预警、库存补货建议、物流异常检测与洞察列表。
     *
     * generate=false（默认）：规则计算；若存在 24 小时内的 AI 结果（Redis）直接回放，source=model；
     * generate=true：强制调用模型重算，模型不可用/超时/校验失败时回退规则结果并保持 200。
     */
    @GetMapping("/business-insights")
    public Result<BusinessInsights> getBusinessInsights(
            @RequestParam(defaultValue = "false") boolean generate) {
        return Result.success(businessInsightService.getBusinessInsights(generate));
    }

    /**
     * 入职离职趋势（M1-5）：按入职/离职日期真实统计
     */
    @GetMapping("/turnover-trend")
    public Result<List<Map<String, Object>>> getTurnoverTrend(
            @RequestParam(defaultValue = "12") int months) {
        return Result.success(analyticsService.getTurnoverTrend(months));
    }

    /**
     * 首页最新动态：由真实的入职、离职、审批记录拼装，按发生时间倒序
     */
    @GetMapping("/recent-activities")
    public Result<List<Map<String, Object>>> getRecentActivities(
            @RequestParam(defaultValue = "6") int limit) {
        return Result.success(analyticsService.getRecentActivities(limit));
    }

    /**
     * 获取招聘分析
     */
    @GetMapping("/recruitment")
    public Result<RecruitmentAnalysis> getRecruitmentAnalysis() {
        return Result.success(analyticsService.getRecruitmentAnalysis());
    }

    /**
     * 获取培训分析
     */
    @GetMapping("/training")
    public Result<TrainingAnalysis> getTrainingAnalysis() {
        return Result.success(analyticsService.getTrainingAnalysis());
    }
}
