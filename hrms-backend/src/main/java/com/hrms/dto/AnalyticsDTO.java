package com.hrms.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 智能分析DTO集合
 */
public class AnalyticsDTO {

    /**
     * 仪表盘统计数据
     */
    @Data
    public static class DashboardStats {
        private Integer totalEmployees;      // 总员工数
        private Integer activeEmployees;     // 在职员工数
        private Integer newEmployees;        // 本月新入职
        private Integer resignedEmployees;   // 本月离职
        private BigDecimal totalSalary;      // 本月薪资总额
        private BigDecimal avgSalary;        // 平均薪资
        private Double attendanceRate;       // 本月出勤率
        private Double avgPerformance;       // 平均绩效分
        private Integer deptCount;           // 部门数量
        private Integer positionCount;       // 岗位数量
        // P8-B 仪表盘新增
        private Integer noticeCount;         // 生效公告数
        private Integer activeUsersToday;    // 今日活跃登录用户数（去重）
    }

    /**
     * 人员结构分析
     */
    @Data
    public static class EmployeeStructure {
        private List<Map<String, Object>> genderDistribution;      // 性别分布
        private List<Map<String, Object>> ageDistribution;         // 年龄分布
        private List<Map<String, Object>> educationDistribution;   // 学历分布
        private List<Map<String, Object>> departmentDistribution;  // 部门分布
        private List<Map<String, Object>> positionDistribution;    // 职位分布
        private List<Map<String, Object>> workYearsDistribution;   // 工龄分布
    }

    /**
     * 考勤分析
     */
    @Data
    public static class AttendanceAnalysis {
        private Double overallAttendanceRate;      // 整体出勤率
        private Double lateRate;                   // 迟到率
        private Double earlyLeaveRate;             // 早退率
        private Double absentRate;                 // 缺勤率
        private Double avgWorkHours;               // 平均工时
        private List<Map<String, Object>> dailyTrend;          // 每日考勤趋势
        private List<Map<String, Object>> deptAttendance;      // 部门考勤对比
        private List<Map<String, Object>> statusDistribution;  // 考勤状态分布
    }

    /**
     * 薪资分析
     */
    @Data
    public static class SalaryAnalysis {
        private BigDecimal totalSalary;            // 薪资总额
        private BigDecimal avgSalary;              // 平均薪资
        private BigDecimal medianSalary;           // 薪资中位数
        private BigDecimal maxSalary;              // 最高薪资
        private BigDecimal minSalary;              // 最低薪资
        private List<Map<String, Object>> salaryDistribution;    // 薪资区间分布
        private List<Map<String, Object>> deptSalaryComparison;  // 部门薪资对比
        private List<Map<String, Object>> salaryTrend;           // 薪资趋势
        private List<Map<String, Object>> salaryStructure;       // 薪资构成
    }

    /**
     * 绩效分析
     */
    @Data
    public static class PerformanceAnalysis {
        private Double avgTotalScore;              // 平均总分
        private List<Map<String, Object>> gradeDistribution;     // 等级分布
        private List<Map<String, Object>> deptPerformance;       // 部门绩效对比
        private List<Map<String, Object>> dimensionAnalysis;     // 维度分析
        private List<Map<String, Object>> performanceTrend;      // 绩效趋势
        private List<Map<String, Object>> topPerformers;         // 优秀员工榜
    }

    /**
     * 人才预测
     */
    @Data
    public static class TalentPrediction {
        private Double predictedAttritionRate;    // 预测离职率
        private List<Map<String, Object>> highRiskEmployees;   // 高风险员工
        private List<Map<String, Object>> promotionCandidates; // 晋升候选人
        private List<Map<String, Object>> recruitmentDemand;   // 招聘需求预测
        private List<Map<String, Object>> futureHeadcount;     // 未来人员预测
    }

    /**
     * AI洞察
     */
    @Data
    public static class AiInsight {
        private String title;                      // 洞察标题
        private String type;                       // 类型：warning/success/info
        private String description;                // 详细描述
        private String suggestion;                 // 建议
        private Double confidence;                 // 置信度
    }

    /**
     * 招聘分析
     */
    @Data
    public static class RecruitmentAnalysis {
        private Integer totalPositions;           // 招聘职位数
        private Integer totalHeadcount;           // 招聘总人数
        private Integer filledPositions;          // 已完成
        private Integer inProgress;               // 进行中
        private List<Map<String, Object>> deptDemand;           // 部门需求分布
        private List<Map<String, Object>> positionDemand;       // 职位需求分布
        private List<Map<String, Object>> timeToHireTrend;      // 招聘周期趋势
    }

    /**
     * 培训分析
     */
    @Data
    public static class TrainingAnalysis {
        private Integer totalTrainings;           // 培训总数
        private Integer completedTrainings;       // 已完成
        private Integer totalParticipants;        // 总参与人次
        private Double avgScore;                  // 平均成绩
        private List<Map<String, Object>> typeDistribution;     // 培训类型分布
        private List<Map<String, Object>> deptParticipation;    // 部门参与度
        private List<Map<String, Object>> trainingEffect;       // 培训效果评估
    }
}
