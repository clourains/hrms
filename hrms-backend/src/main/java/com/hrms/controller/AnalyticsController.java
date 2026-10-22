package com.hrms.controller;

import com.hrms.dto.AnalyticsDTO.*;
import com.hrms.service.AnalyticsService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能分析控制器
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

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
     * 获取AI洞察
     */
    @GetMapping("/ai-insights")
    public Result<List<AiInsight>> getAiInsights() {
        return Result.success(analyticsService.getAiInsights());
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
