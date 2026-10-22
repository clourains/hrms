package com.hrms.service;

import com.hrms.dto.AnalyticsDTO.*;

/**
 * 智能分析服务接口
 */
public interface AnalyticsService {

    /**
     * 获取仪表盘统计数据
     */
    DashboardStats getDashboardStats();

    /**
     * 获取人员结构分析
     */
    EmployeeStructure getEmployeeStructure();

    /**
     * 获取考勤分析
     * @param month 月份 yyyy-MM
     */
    AttendanceAnalysis getAttendanceAnalysis(String month);

    /**
     * 获取薪资分析
     * @param month 月份 yyyy-MM
     */
    SalaryAnalysis getSalaryAnalysis(String month);

    /**
     * 获取绩效分析
     * @param period 周期 yyyy-MM
     */
    PerformanceAnalysis getPerformanceAnalysis(String period);

    /**
     * 获取人才预测
     */
    TalentPrediction getTalentPrediction();

    /**
     * 获取AI洞察
     */
    java.util.List<AiInsight> getAiInsights();

    /**
     * 获取招聘分析
     */
    RecruitmentAnalysis getRecruitmentAnalysis();

    /**
     * 获取培训分析
     */
    TrainingAnalysis getTrainingAnalysis();
}
