package com.hrms.service.impl;

import com.hrms.dto.AnalyticsDTO.*;
import com.hrms.entity.*;
import com.hrms.mapper.*;
import com.hrms.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能分析服务实现类
 */
@Slf4j
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    /** 部门离职预警的最小样本量：人数不足 5 人的部门不参与比较，避免「3 人走 1 人 = 33%」这种噪声 */
    private static final int MIN_DEPT_FOR_ALERT = 5;

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
    private NoticeMapper noticeMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private JobRequisitionMapper jobRequisitionMapper;

    @Autowired
    private ApprovalInstanceMapper approvalInstanceMapper;

    /**
     * 入职离职趋势（M1-5）：按入职日期/离职日期真实按月聚合。
     * 离职日期为新列 leave_date，历史数据为空时离职线自然为 0，
     * 随着办理离职填写离职日期，曲线逐步积累真实数据。
     */
    @Override
    public List<Map<String, Object>> getTurnoverTrend(int months) {
        int range = Math.max(1, Math.min(months, 24));
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);

        List<Employee> allEmployees = employeeMapper.selectList(null);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = range - 1; i >= 0; i--) {
            String monthKey = currentMonthStart.minusMonths(i).format(monthFmt);
            long hireCount = allEmployees.stream()
                    .filter(e -> e.getEntryDate() != null
                            && e.getEntryDate().format(monthFmt).equals(monthKey))
                    .count();
            long leaveCount = allEmployees.stream()
                    .filter(e -> e.getLeaveDate() != null
                            && e.getLeaveDate().format(monthFmt).equals(monthKey))
                    .count();
            Map<String, Object> item = new HashMap<>();
            item.put("month", monthKey);
            item.put("hireCount", hireCount);
            item.put("leaveCount", leaveCount);
            trend.add(item);
        }
        return trend;
    }

    @Override
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        
        // 员工统计
        List<Employee> allEmployees = employeeMapper.selectList(null);
        stats.setTotalEmployees(allEmployees.size());
        stats.setActiveEmployees((int) allEmployees.stream().filter(e -> e.getStatus() == 1).count());
        
        // 本月新入职
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        stats.setNewEmployees((int) allEmployees.stream()
                .filter(e -> e.getEntryDate() != null && 
                        e.getEntryDate().format(DateTimeFormatter.ofPattern("yyyy-MM")).equals(currentMonth))
                .count());
        
        // 离职员工
        stats.setResignedEmployees((int) allEmployees.stream().filter(e -> e.getStatus() == 0).count());
        
        // 部门数量
        stats.setDeptCount(departmentMapper.selectCount(null).intValue());
        
        // 岗位数量
        Set<String> positions = allEmployees.stream()
                .map(Employee::getPosition)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        stats.setPositionCount(positions.size());
        
        // 薪资统计
        BigDecimal totalSalary = allEmployees.stream()
                .map(Employee::getSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalSalary(totalSalary);
        stats.setAvgSalary(allEmployees.isEmpty() ? BigDecimal.ZERO : 
                totalSalary.divide(BigDecimal.valueOf(allEmployees.size()), 2, RoundingMode.HALF_UP));
        
        // 出勤率 - 从考勤表真实计算
        List<Attendance> allAttendance = attendanceMapper.selectList(null);
        if (!allAttendance.isEmpty()) {
            long normalCount = allAttendance.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count();
            stats.setAttendanceRate(round(normalCount * 100.0 / allAttendance.size(), 2));
        } else {
            stats.setAttendanceRate(0.0);
        }

        // 平均绩效 - 从绩效表真实计算
        List<Performance> allPerformances = performanceMapper.selectList(null);
        if (!allPerformances.isEmpty()) {
            OptionalDouble avgScore = allPerformances.stream()
                    .filter(p -> p.getTotalScore() != null)
                    .mapToDouble(p -> p.getTotalScore().doubleValue())
                    .average();
            stats.setAvgPerformance(round(avgScore.orElse(0), 2));
        } else {
            stats.setAvgPerformance(0.0);
        }

        // P8-B 仪表盘扩展：生效公告数 + 今日活跃登录用户数
        stats.setNoticeCount(Math.toIntExact(noticeMapper.selectCount(
                new LambdaQueryWrapper<Notice>().eq(Notice::getStatus, 1))));
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay().minusSeconds(1);
        List<LoginLog> todayLogs = loginLogMapper.selectList(
                new LambdaQueryWrapper<LoginLog>()
                        .eq(LoginLog::getSuccess, 1)
                        .ge(LoginLog::getCreateTime, dayStart)
                        .le(LoginLog::getCreateTime, dayEnd));
        stats.setActiveUsersToday((int) todayLogs.stream()
                .map(LoginLog::getUsername).filter(Objects::nonNull).distinct().count());

        return stats;
    }

    @Override
    public EmployeeStructure getEmployeeStructure() {
        EmployeeStructure structure = new EmployeeStructure();
        List<Employee> employees = employeeMapper.selectList(null);
        List<Department> departments = departmentMapper.selectList(null);
        Map<Long, String> deptMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        // 性别分布
        long maleCount = employees.stream().filter(e -> e.getGender() != null && e.getGender() == 1).count();
        long femaleCount = employees.stream().filter(e -> e.getGender() != null && e.getGender() == 0).count();
        List<Map<String, Object>> genderList = new ArrayList<>();
        genderList.add(createMap("name", "男", "value", maleCount));
        genderList.add(createMap("name", "女", "value", femaleCount));
        structure.setGenderDistribution(genderList);

        // 年龄分布
        Map<String, Long> ageGroups = employees.stream()
                .filter(e -> e.getBirthday() != null)
                .collect(Collectors.groupingBy(e -> {
                    int age = LocalDate.now().getYear() - e.getBirthday().getYear();
                    if (age < 25) return "25岁以下";
                    if (age < 30) return "25-30岁";
                    if (age < 35) return "30-35岁";
                    if (age < 40) return "35-40岁";
                    if (age < 45) return "40-45岁";
                    return "45岁以上";
                }, Collectors.counting()));
        structure.setAgeDistribution(sortAndConvertMap(ageGroups, 
                Arrays.asList("25岁以下", "25-30岁", "30-35岁", "35-40岁", "40-45岁", "45岁以上")));

        // 学历分布
        Map<String, Long> eduGroups = employees.stream()
                .filter(e -> e.getEducation() != null)
                .collect(Collectors.groupingBy(Employee::getEducation, Collectors.counting()));
        structure.setEducationDistribution(sortAndConvertMap(eduGroups, 
                Arrays.asList("博士", "硕士", "本科", "大专", "高中及以下")));

        // 部门分布
        Map<String, Long> deptGroups = employees.stream()
                .filter(e -> e.getDeptId() != null)
                .collect(Collectors.groupingBy(
                        e -> deptMap.getOrDefault(e.getDeptId(), "未分配"),
                        Collectors.counting()));
        structure.setDepartmentDistribution(convertMapToList(deptGroups));

        // 职位分布
        Map<String, Long> posGroups = employees.stream()
                .filter(e -> e.getPosition() != null)
                .collect(Collectors.groupingBy(Employee::getPosition, Collectors.counting()));
        structure.setPositionDistribution(convertMapToList(posGroups));

        // 工龄分布
        Map<String, Long> workYearsGroups = employees.stream()
                .collect(Collectors.groupingBy(e -> {
                    Integer years = e.getWorkYears();
                    if (years == null) return "未知";
                    if (years < 1) return "1年以下";
                    if (years < 3) return "1-3年";
                    if (years < 5) return "3-5年";
                    if (years < 10) return "5-10年";
                    return "10年以上";
                }, Collectors.counting()));
        structure.setWorkYearsDistribution(sortAndConvertMap(workYearsGroups,
                Arrays.asList("1年以下", "1-3年", "3-5年", "5-10年", "10年以上")));

        return structure;
    }

    @Override
    public AttendanceAnalysis getAttendanceAnalysis(String month) {
        AttendanceAnalysis analysis = new AttendanceAnalysis();
        List<Attendance> attendances = attendanceMapper.selectList(null);
        
        // 筛选当月数据
        if (month != null && !month.isEmpty()) {
            attendances = attendances.stream()
                    .filter(a -> a.getAttendDate() != null && 
                            a.getAttendDate().format(DateTimeFormatter.ofPattern("yyyy-MM")).equals(month))
                    .collect(Collectors.toList());
        }

        if (!attendances.isEmpty()) {
            long total = attendances.size();
            long normal = attendances.stream().filter(a -> a.getStatus() == 1).count();
            long late = attendances.stream().filter(a -> a.getStatus() == 2).count();
            long early = attendances.stream().filter(a -> a.getStatus() == 3).count();
            long absent = attendances.stream().filter(a -> a.getStatus() == 4).count();

            analysis.setOverallAttendanceRate(round(normal * 100.0 / total, 2));
            analysis.setLateRate(round(late * 100.0 / total, 2));
            analysis.setEarlyLeaveRate(round(early * 100.0 / total, 2));
            analysis.setAbsentRate(round(absent * 100.0 / total, 2));

            // 平均工时
            OptionalDouble avgHours = attendances.stream()
                    .filter(a -> a.getWorkHours() != null)
                    .mapToDouble(a -> a.getWorkHours().doubleValue())
                    .average();
            analysis.setAvgWorkHours(round(avgHours.orElse(0), 2));
        }

        // 考勤状态分布
        List<Map<String, Object>> statusList = new ArrayList<>();
        statusList.add(createMap("name", "正常", "value", attendances.stream().filter(a -> a.getStatus() == 1).count()));
        statusList.add(createMap("name", "迟到", "value", attendances.stream().filter(a -> a.getStatus() == 2).count()));
        statusList.add(createMap("name", "早退", "value", attendances.stream().filter(a -> a.getStatus() == 3).count()));
        statusList.add(createMap("name", "缺勤", "value", attendances.stream().filter(a -> a.getStatus() == 4).count()));
        statusList.add(createMap("name", "请假", "value", attendances.stream().filter(a -> a.getStatus() == 5).count()));
        statusList.add(createMap("name", "出差", "value", attendances.stream().filter(a -> a.getStatus() == 6).count()));
        analysis.setStatusDistribution(statusList);

        // 每日趋势（最近30天）- 基于真实考勤数据计算
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<Attendance> dayRecords = attendances.stream()
                    .filter(a -> a.getAttendDate() != null && a.getAttendDate().equals(date))
                    .collect(Collectors.toList());
            long dayCount = dayRecords.size();
            long dayNormal = dayRecords.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count();
            double dayRate = dayCount > 0 ? round(dayNormal * 100.0 / dayCount, 2) : 0;
            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("date", date.toString());
            dayMap.put("count", dayCount);
            dayMap.put("attendanceRate", dayRate);
            dailyTrend.add(dayMap);
        }
        analysis.setDailyTrend(dailyTrend);

        // 部门考勤对比 - 基于真实数据计算
        List<Department> departments = departmentMapper.selectList(null);
        List<Employee> allEmps = employeeMapper.selectList(null);
        Map<Long, Long> empDeptMap = allEmps.stream()
                .filter(e -> e.getDeptId() != null)
                .collect(Collectors.groupingBy(Employee::getDeptId, Collectors.counting()));
        List<Map<String, Object>> deptAttendance = new ArrayList<>();
        for (Department dept : departments) {
            if (dept.getParentId() != null && dept.getParentId() == 0) continue;
            List<Attendance> deptAtt = attendances.stream()
                    .filter(a -> {
                        Employee emp = allEmps.stream().filter(e -> e.getId().equals(a.getEmpId())).findFirst().orElse(null);
                        return emp != null && emp.getDeptId() != null && emp.getDeptId().equals(dept.getId());
                    })
                    .collect(Collectors.toList());
            long deptTotal = deptAtt.size();
            long deptNormal = deptAtt.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count();
            long deptLate = deptAtt.stream().filter(a -> a.getStatus() != null && a.getStatus() == 2).count();
            OptionalDouble deptAvgHours = deptAtt.stream()
                    .filter(a -> a.getWorkHours() != null)
                    .mapToDouble(a -> a.getWorkHours().doubleValue())
                    .average();
            Map<String, Object> deptMap = new LinkedHashMap<>();
            deptMap.put("name", dept.getDeptName());
            deptMap.put("attendanceRate", deptTotal > 0 ? round(deptNormal * 100.0 / deptTotal, 2) : 0);
            deptMap.put("lateRate", deptTotal > 0 ? round(deptLate * 100.0 / deptTotal, 2) : 0);
            deptMap.put("avgWorkHours", round(deptAvgHours.orElse(0), 2));
            deptAttendance.add(deptMap);
        }
        analysis.setDeptAttendance(deptAttendance);

        return analysis;
    }

    @Override
    public SalaryAnalysis getSalaryAnalysis(String month) {
        SalaryAnalysis analysis = new SalaryAnalysis();
        List<Salary> salaries = salaryMapper.selectList(null);

        // 过滤：排除关联员工已删除/不存在的工资记录
        // 状态口径：1=在职、2=试用期（两者都发薪）、0=离职；只排除离职，否则试用期员工
        // 会被漏掉，导致薪资分布人数少于员工总数（口径对不上）。
        List<Employee> allEmployees = employeeMapper.selectList(null);
        Set<Long> validEmpIds = allEmployees.stream()
                .filter(e -> e.getStatus() != null && e.getStatus() != 0)
                .map(Employee::getId)
                .collect(Collectors.toSet());
        salaries = salaries.stream()
                .filter(s -> validEmpIds.contains(s.getEmpId()))
                .collect(Collectors.toList());

        // 筛选当月数据：未传 month 时默认取"有有效薪资数据"的最新月份
        // （跳过本月工资单尚未发放、net_salary 全为 0 的情况）
        if (month == null || month.isEmpty()) {
            month = salaries.stream()
                    .filter(s -> s.getNetSalary() != null && s.getNetSalary().compareTo(BigDecimal.ZERO) > 0)
                    .map(Salary::getSalaryMonth)
                    .filter(Objects::nonNull)
                    .max(String::compareTo)
                    .orElse(salaries.stream()
                            .map(Salary::getSalaryMonth)
                            .filter(Objects::nonNull)
                            .max(String::compareTo)
                            .orElse(null));
        }
        if (month != null && !month.isEmpty()) {
            String finalMonth = month;
            salaries = salaries.stream()
                    .filter(s -> finalMonth.equals(s.getSalaryMonth()))
                    .collect(Collectors.toList());
        }

        if (!salaries.isEmpty()) {
            // 总额和平均值
            BigDecimal total = salaries.stream()
                    .map(Salary::getNetSalary)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            analysis.setTotalSalary(total);
            analysis.setAvgSalary(total.divide(BigDecimal.valueOf(salaries.size()), 2, RoundingMode.HALF_UP));

            // 最高/最低
            Optional<BigDecimal> maxSal = salaries.stream()
                    .map(Salary::getNetSalary)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo);
            Optional<BigDecimal> minSal = salaries.stream()
                    .map(Salary::getNetSalary)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo);
            analysis.setMaxSalary(maxSal.orElse(BigDecimal.ZERO));
            analysis.setMinSalary(minSal.orElse(BigDecimal.ZERO));

            // 中位数（简化计算）
            List<BigDecimal> sortedSalaries = salaries.stream()
                    .map(Salary::getNetSalary)
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());
            int mid = sortedSalaries.size() / 2;
            analysis.setMedianSalary(sortedSalaries.get(mid));
        }

        // 薪资区间分布
        List<Map<String, Object>> salaryDist = new ArrayList<>();
        String[] ranges = {"5k以下", "5k-10k", "10k-15k", "15k-20k", "20k-30k", "30k以上"};
        long[] counts = new long[6];
        for (Salary s : salaries) {
            BigDecimal sal = s.getNetSalary();
            if (sal == null) continue;
            double val = sal.doubleValue();
            if (val < 5000) counts[0]++;
            else if (val < 10000) counts[1]++;
            else if (val < 15000) counts[2]++;
            else if (val < 20000) counts[3]++;
            else if (val < 30000) counts[4]++;
            else counts[5]++;
        }
        for (int i = 0; i < ranges.length; i++) {
            salaryDist.add(createMap("name", ranges[i], "value", counts[i]));
        }
        analysis.setSalaryDistribution(salaryDist);

        // 部门薪资对比
        List<Employee> employees = employeeMapper.selectList(null);
        Map<Long, Employee> empMap = employees.stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        List<Department> departments = departmentMapper.selectList(null);
        Map<Long, String> deptMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        Map<String, List<BigDecimal>> deptSalMap = new HashMap<>();
        for (Salary s : salaries) {
            Employee emp = empMap.get(s.getEmpId());
            if (emp != null && emp.getDeptId() != null) {
                String deptName = deptMap.getOrDefault(emp.getDeptId(), "其他");
                deptSalMap.computeIfAbsent(deptName, k -> new ArrayList<>()).add(s.getNetSalary());
            }
        }

        List<Map<String, Object>> deptSalaryList = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : deptSalMap.entrySet()) {
            BigDecimal avg = entry.getValue().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", entry.getKey());
            map.put("avgSalary", avg);
            map.put("headcount", entry.getValue().size());
            deptSalaryList.add(map);
        }
        analysis.setDeptSalaryComparison(deptSalaryList);

        // 薪资趋势（最近6个月）- 基于真实数据计算
        List<Salary> allSalaries = salaryMapper.selectList(null);
        List<Map<String, Object>> salaryTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            String m = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            List<Salary> monthSalaries = allSalaries.stream()
                    .filter(s -> m.equals(s.getSalaryMonth()))
                    .collect(Collectors.toList());
            BigDecimal monthTotal = monthSalaries.stream()
                    .map(Salary::getNetSalary)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal monthAvg = monthSalaries.isEmpty() ? BigDecimal.ZERO :
                    monthTotal.divide(BigDecimal.valueOf(monthSalaries.size()), 2, RoundingMode.HALF_UP);
            Map<String, Object> monthMap = new LinkedHashMap<>();
            monthMap.put("month", m);
            monthMap.put("totalSalary", monthTotal);
            monthMap.put("avgSalary", monthAvg);
            salaryTrend.add(monthMap);
        }
        analysis.setSalaryTrend(salaryTrend);

        // 薪资构成
        List<Map<String, Object>> structure = new ArrayList<>();
        structure.add(createMap("name", "基本工资", "value", 65));
        structure.add(createMap("name", "绩效奖金", "value", 15));
        structure.add(createMap("name", "加班费", "value", 5));
        structure.add(createMap("name", "补贴", "value", 5));
        structure.add(createMap("name", "社保公积金", "value", 10));
        analysis.setSalaryStructure(structure);

        return analysis;
    }

    @Override
    public PerformanceAnalysis getPerformanceAnalysis(String period) {
        PerformanceAnalysis analysis = new PerformanceAnalysis();
        List<Performance> performances = performanceMapper.selectList(null);

        if (period != null && !period.isEmpty()) {
            performances = performances.stream()
                    .filter(p -> period.equals(p.getPeriod()))
                    .collect(Collectors.toList());
        }

        // 平均总分
        if (!performances.isEmpty()) {
            OptionalDouble avgScore = performances.stream()
                    .filter(p -> p.getTotalScore() != null)
                    .mapToDouble(p -> p.getTotalScore().doubleValue())
                    .average();
            analysis.setAvgTotalScore(round(avgScore.orElse(0), 2));
        }

        // 等级分布
        Map<String, Long> gradeMap = performances.stream()
                .filter(p -> p.getGrade() != null)
                .collect(Collectors.groupingBy(Performance::getGrade, Collectors.counting()));
        analysis.setGradeDistribution(sortAndConvertMap(gradeMap, Arrays.asList("S", "A", "B", "C", "D")));

        // 维度分析
        List<Map<String, Object>> dimensionList = new ArrayList<>();
        dimensionList.add(createMap("name", "工作质量", "avg", calculateAvg(performances, "quality")));
        dimensionList.add(createMap("name", "工作效率", "avg", calculateAvg(performances, "efficiency")));
        dimensionList.add(createMap("name", "工作态度", "avg", calculateAvg(performances, "attitude")));
        dimensionList.add(createMap("name", "团队协作", "avg", calculateAvg(performances, "teamwork")));
        dimensionList.add(createMap("name", "创新能力", "avg", calculateAvg(performances, "innovation")));
        analysis.setDimensionAnalysis(dimensionList);

        // 部门绩效对比
        List<Employee> employees = employeeMapper.selectList(null);
        Map<Long, Employee> empMap = employees.stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        List<Department> departments = departmentMapper.selectList(null);
        Map<Long, String> deptMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        Map<String, List<BigDecimal>> deptPerfMap = new HashMap<>();
        for (Performance p : performances) {
            Employee emp = empMap.get(p.getEmpId());
            if (emp != null && emp.getDeptId() != null) {
                String deptName = deptMap.getOrDefault(emp.getDeptId(), "其他");
                deptPerfMap.computeIfAbsent(deptName, k -> new ArrayList<>()).add(p.getTotalScore());
            }
        }

        List<Map<String, Object>> deptPerfList = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : deptPerfMap.entrySet()) {
            BigDecimal avg = entry.getValue().stream()
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", entry.getKey());
            map.put("avgScore", avg);
            deptPerfList.add(map);
        }
        analysis.setDeptPerformance(deptPerfList);

        // 绩效趋势（最近6个月）- 基于真实数据计算
        List<Performance> allPerfs = performanceMapper.selectList(null);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            String m = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            List<Performance> monthPerfs = allPerfs.stream()
                    .filter(p -> m.equals(p.getPeriod()))
                    .collect(Collectors.toList());
            OptionalDouble monthAvg = monthPerfs.stream()
                    .filter(p -> p.getTotalScore() != null)
                    .mapToDouble(p -> p.getTotalScore().doubleValue())
                    .average();
            long excellentCount = monthPerfs.stream()
                    .filter(p -> "S".equals(p.getGrade()) || "A".equals(p.getGrade()))
                    .count();
            double excellentRate = monthPerfs.isEmpty() ? 0 : round(excellentCount * 100.0 / monthPerfs.size(), 2);
            Map<String, Object> monthMap = new LinkedHashMap<>();
            monthMap.put("period", m);
            monthMap.put("avgScore", round(monthAvg.orElse(0), 2));
            monthMap.put("excellentRate", excellentRate);
            trend.add(monthMap);
        }
        analysis.setPerformanceTrend(trend);

        // 优秀员工榜
        List<Performance> sorted = performances.stream()
                .filter(p -> p.getTotalScore() != null)
                .sorted((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()))
                .limit(10)
                .collect(Collectors.toList());
        List<Map<String, Object>> topList = new ArrayList<>();
        for (Performance p : sorted) {
            Employee emp = empMap.get(p.getEmpId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("empId", p.getEmpId());
            map.put("name", emp != null ? emp.getName() : "未知");
            map.put("position", emp != null ? emp.getPosition() : "");
            map.put("deptName", emp != null ? deptMap.getOrDefault(emp.getDeptId(), "") : "");
            map.put("totalScore", p.getTotalScore());
            map.put("grade", p.getGrade());
            topList.add(map);
        }
        analysis.setTopPerformers(topList);

        return analysis;
    }

    @Override
    public TalentPrediction getTalentPrediction() {
        TalentPrediction prediction = new TalentPrediction();
        List<Employee> employees = employeeMapper.selectList(null);
        List<Employee> activeEmps = employees.stream()
                .filter(e -> e.getStatus() != null && e.getStatus() == 1)
                .collect(Collectors.toList());
        List<Performance> performances = performanceMapper.selectList(null);
        Map<Long, Performance> perfMap = new HashMap<>();
        for (Performance p : performances) {
            perfMap.putIfAbsent(p.getEmpId(), p);
        }

        // 近 6 个月滚动离职率 = 近 6 个月离职人数 ÷ 当前在职人数
        // 口径说明：原实现用「累计离职人数 ÷ 员工总数」，那是存量比率（历史沉淀），
        // 不能反映近期趋势，也与字段名 predictedAttritionRate 不符，故改为滚动口径。
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        long recentResigned = employees.stream()
                .filter(e -> e.getLeaveDate() != null && !e.getLeaveDate().isBefore(sixMonthsAgo))
                .count();
        double attritionRate = activeEmps.isEmpty() ? 0
                : round(recentResigned * 100.0 / activeEmps.size(), 2);
        prediction.setPredictedAttritionRate(attritionRate);

        // 高风险员工 - 基于工龄、绩效、薪资等因素计算风险评分
        List<Map<String, Object>> highRisk = new ArrayList<>();
        List<Map.Entry<Employee, Double>> riskyEmployees = activeEmps.stream()
                .map(e -> {
                    double riskScore = calculateRiskScore(e, perfMap.get(e.getId()), activeEmps);
                    return new AbstractMap.SimpleEntry<>(e, riskScore);
                })
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .collect(Collectors.toList());
        for (Map.Entry<Employee, Double> entry : riskyEmployees) {
            Employee emp = entry.getKey();
            double score = entry.getValue();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("empId", emp.getId());
            map.put("name", emp.getName());
            map.put("position", emp.getPosition());
            map.put("workYears", emp.getWorkYears());
            map.put("riskLevel", score > 70 ? "高" : score > 50 ? "中" : "低");
            map.put("riskScore", round(score, 2));
            List<String> factors = new ArrayList<>();
            if (emp.getWorkYears() != null && emp.getWorkYears() < 2) factors.add("入职时间短");
            if (emp.getSalary() != null && emp.getSalary().doubleValue() < activeEmps.stream()
                    .mapToDouble(e2 -> e2.getSalary() != null ? e2.getSalary().doubleValue() : 0)
                    .average().orElse(0)) factors.add("薪资偏低");
            Performance p = perfMap.get(emp.getId());
            if (p != null && p.getTotalScore() != null && p.getTotalScore().doubleValue() < 75) factors.add("绩效偏低");
            if (factors.isEmpty()) factors.add("综合因素");
            map.put("riskFactors", factors);
            highRisk.add(map);
        }
        prediction.setHighRiskEmployees(highRisk);

        // 晋升候选人 - 基于工龄、绩效计算潜力评分
        List<Map<String, Object>> promotions = new ArrayList<>();
        List<Map.Entry<Employee, Double>> candidates = activeEmps.stream()
                .filter(e -> e.getWorkYears() != null && e.getWorkYears() >= 3)
                .map(e -> {
                    double potential = calculatePotentialScore(e, perfMap.get(e.getId()));
                    return new AbstractMap.SimpleEntry<>(e, potential);
                })
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(8)
                .collect(Collectors.toList());
        for (Map.Entry<Employee, Double> entry : candidates) {
            Employee emp = entry.getKey();
            double score = entry.getValue();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("empId", emp.getId());
            map.put("name", emp.getName());
            map.put("currentPosition", emp.getPosition());
            map.put("suggestedPosition", "高级" + (emp.getPosition() != null ? emp.getPosition() : "专员"));
            map.put("workYears", emp.getWorkYears());
            map.put("potentialScore", round(score, 2));
            promotions.add(map);
        }
        prediction.setPromotionCandidates(promotions);

        // 招聘需求：取「在招需求」的真实缺口 = 需求人数 headcount - 已招人数 hiredCount
        // 口径说明：原实现用 ceil(现有部门人数 × 10%)、不足 3 人给 1，与编制、
        // 在招计划都无关，属拍脑袋估算；现改为读招聘需求表，可追溯到具体需求单。
        List<Department> departments = departmentMapper.selectList(null);
        Map<Long, List<Employee>> deptEmpMap = activeEmps.stream()
                .filter(e -> e.getDeptId() != null)
                .collect(Collectors.groupingBy(Employee::getDeptId));

        Map<Long, int[]> gapByDept = new HashMap<>();   // deptId -> [缺口人数, 在招需求单数]
        try {
            List<JobRequisition> openReqs = jobRequisitionMapper.selectList(
                    new LambdaQueryWrapper<JobRequisition>().eq(JobRequisition::getStatus, 1));
            for (JobRequisition r : openReqs) {
                if (r.getDeptId() == null) continue;
                int need = r.getHeadcount() == null ? 0 : r.getHeadcount();
                int hired = r.getHiredCount() == null ? 0 : r.getHiredCount();
                int gap = Math.max(0, need - hired);
                if (gap <= 0) continue;
                int[] acc = gapByDept.computeIfAbsent(r.getDeptId(), k -> new int[2]);
                acc[0] += gap;
                acc[1] += 1;
            }
        } catch (Exception e) {
            log.warn("取在招需求失败：{}", e.getMessage());
        }

        List<Map<String, Object>> recruitment = new ArrayList<>();
        for (Department dept : departments) {
            if (dept.getParentId() != null && dept.getParentId() == 0) continue;
            List<Employee> deptEmps = deptEmpMap.getOrDefault(dept.getId(), Collections.emptyList());
            int currentCount = deptEmps.size();
            int[] acc = gapByDept.get(dept.getId());
            int predictedNeed = acc == null ? 0 : acc[0];
            int reqCount = acc == null ? 0 : acc[1];
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("position", dept.getDeptName());
            map.put("currentCount", currentCount);
            map.put("predictedDemand", predictedNeed);
            map.put("openRequisitions", reqCount);
            map.put("priority", predictedNeed >= 3 ? "高" : predictedNeed >= 1 ? "中" : "无");
            map.put("basis", predictedNeed > 0 ? "在招需求缺口" : "无在招需求");
            recruitment.add(map);
        }
        // 缺口大的排前面，无需求的沉底
        recruitment.sort((a, b) -> Integer.compare(
                (Integer) b.get("predictedDemand"), (Integer) a.get("predictedDemand")));
        prediction.setRecruitmentDemand(recruitment);

        // 未来人员预测（未来 12 个月）：依据近 12 个月真实入离职均值外推
        // 口径说明：原实现含 `i * 1.5`（硬编码每月新增 1.5 人）与 `attritions + (i<3 ? 2 : 1)`，
        // 与任何数据无关，等于凭空造出上升趋势；现改为「当前在职 + 累计入职 - 累计离职」，
        // 入离职均取近 12 个月真实记录，累计值用整数分摊保证 12 个月合计等于年真实值。
        List<Map<String, Object>> futureHeadcount = new ArrayList<>();
        LocalDate oneYearAgo = LocalDate.now().minusMonths(12);
        long hiresLastYear = employees.stream()
                .filter(e -> e.getEntryDate() != null
                        && !e.getEntryDate().isBefore(oneYearAgo)
                        && !e.getEntryDate().isAfter(LocalDate.now()))
                .count();
        long leavesLastYear = employees.stream()
                .filter(e -> e.getLeaveDate() != null && !e.getLeaveDate().isBefore(oneYearAgo))
                .count();
        int baseCount = activeEmps.size();
        int cumHiresPrev = 0;
        int cumLeavesPrev = 0;
        for (int i = 1; i <= 12; i++) {
            LocalDate date = LocalDate.now().plusMonths(i);
            int cumHires = (int) Math.round((double) hiresLastYear * i / 12);
            int cumLeaves = (int) Math.round((double) leavesLastYear * i / 12);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("month", date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            map.put("predictedCount", Math.max(0, baseCount + cumHires - cumLeaves));
            map.put("newHires", cumHires - cumHiresPrev);
            map.put("attritions", cumLeaves - cumLeavesPrev);
            futureHeadcount.add(map);
            cumHiresPrev = cumHires;
            cumLeavesPrev = cumLeaves;
        }
        prediction.setFutureHeadcount(futureHeadcount);

        return prediction;
    }

    private double calculateRiskScore(Employee emp, Performance perf, List<Employee> allEmps) {
        double score = 30;
        if (emp.getWorkYears() != null) {
            if (emp.getWorkYears() < 1) score += 25;
            else if (emp.getWorkYears() < 3) score += 15;
            else if (emp.getWorkYears() > 10) score += 10;
        }
        if (emp.getSalary() != null) {
            double avgSalary = allEmps.stream()
                    .mapToDouble(e -> e.getSalary() != null ? e.getSalary().doubleValue() : 0)
                    .average().orElse(0);
            if (emp.getSalary().doubleValue() < avgSalary * 0.8) score += 20;
            else if (emp.getSalary().doubleValue() < avgSalary) score += 10;
        }
        if (perf != null && perf.getTotalScore() != null) {
            if (perf.getTotalScore().doubleValue() < 70) score += 20;
            else if (perf.getTotalScore().doubleValue() < 80) score += 10;
        }
        return Math.min(score, 95);
    }

    private double calculatePotentialScore(Employee emp, Performance perf) {
        double score = 50;
        if (emp.getWorkYears() != null) {
            score += Math.min(emp.getWorkYears() * 3, 20);
        }
        if (perf != null && perf.getTotalScore() != null) {
            score += (perf.getTotalScore().doubleValue() - 70) * 0.8;
        }
        if (emp.getEducation() != null) {
            if (emp.getEducation().equals("博士")) score += 10;
            else if (emp.getEducation().equals("硕士")) score += 7;
            else if (emp.getEducation().equals("本科")) score += 5;
        }
        return Math.min(Math.max(score, 0), 100);
    }

    @Override
    public List<AiInsight> getAiInsights() {
        List<AiInsight> insights = new ArrayList<>();
        List<Employee> employees = employeeMapper.selectList(null);
        List<Employee> activeEmps = employees.stream()
                .filter(e -> e.getStatus() != null && e.getStatus() == 1)
                .collect(Collectors.toList());
        List<Performance> performances = performanceMapper.selectList(null);
        List<Attendance> attendances = attendanceMapper.selectList(null);
        List<Salary> salaries = salaryMapper.selectList(null);
        List<Department> departments = departmentMapper.selectList(null);

        // 洞察1: 部门离职风险（key 用 deptId，便于给出带部门筛选的跳转链接）
        Map<Long, String> deptMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));
        Map<Long, long[]> deptResignMap = new HashMap<>();
        for (Employee e : employees) {
            if (e.getDeptId() == null) continue;
            long[] arr = deptResignMap.computeIfAbsent(e.getDeptId(), k -> new long[2]);
            arr[0]++;
            if (e.getStatus() != null && e.getStatus() == 0) arr[1]++;
        }
        Long highestDeptId = null;
        double highestResignRate = 0;
        long highestDeptTotal = 0;
        long highestDeptLeft = 0;
        for (Map.Entry<Long, long[]> entry : deptResignMap.entrySet()) {
            long total = entry.getValue()[0];
            // 部门人数不足 5 人不参与预警：3 人里走 1 人就是 33%，小样本没有统计意义
            if (total < MIN_DEPT_FOR_ALERT) continue;
            double rate = entry.getValue()[1] * 100.0 / total;
            if (rate > highestResignRate) {
                highestResignRate = rate;
                highestDeptId = entry.getKey();
                highestDeptTotal = total;
                highestDeptLeft = entry.getValue()[1];
            }
        }
        String highestResignDept = highestDeptId == null ? null : deptMap.getOrDefault(highestDeptId, "其他");
        if (highestResignDept != null && highestResignRate > 0) {
            AiInsight insight = new AiInsight();
            insight.setTitle(highestResignDept + "离职风险预警");
            insight.setType("warning");
            // 对照用：公司滚动离职率，口径与 talentPrediction 完全一致，避免「高于平均水平」成为无据断言
            long recentResigned = employees.stream()
                    .filter(e -> e.getLeaveDate() != null && !e.getLeaveDate().isBefore(LocalDate.now().minusMonths(6)))
                    .count();
            double companyRate = activeEmps.isEmpty() ? 0 : round(recentResigned * 100.0 / activeEmps.size(), 2);
            insight.setDescription(String.format("%s离职率达到%.1f%%，%s。建议关注该部门员工稳定性。",
                    highestResignDept, highestResignRate,
                    highestResignRate > companyRate ? "高于公司近 6 个月水平" : "与公司近 6 个月水平基本持平"));
            insight.setSuggestion("建议：1. 分析离职原因；2. 优化薪酬结构；3. 建立晋升通道。");
            insight.setConfidence(0.85);
            insight.setCategory("turnover");
            // 依据与文案同源：直接用算出该百分比的两个原始计数，便于逐条核对
            insight.setEvidence(new ArrayList<>(List.of(
                    createMap("label", highestResignDept + "员工数（含已离职）", "value", highestDeptTotal + " 人", "source", "sys_employee.dept_id"),
                    createMap("label", highestResignDept + "离职人数", "value", highestDeptLeft + " 人", "source", "sys_employee.status = 0"),
                    createMap("label", "部门离职率", "value", String.format("%.1f%%", highestResignRate), "source", "离职人数 ÷ 员工数"),
                    createMap("label", "公司近 6 个月离职率（对照）", "value", String.format("%.1f%%", companyRate),
                            "source", String.format("近 6 个月离职 %d 人 ÷ 在职 %d 人", recentResigned, activeEmps.size()))
            )));
            insight.setBasis("部门离职率 = 该部门离职人数 ÷ 该部门员工数（含已离职）；"
                    + "对照行是公司口径 = 近 6 个月离职人数 ÷ 当前在职人数，两者分母不同，只用于判断「是否偏高」，不可直接相减。");
            insight.setTargets(new ArrayList<>(List.of(
                    createMap("label", "查看" + highestResignDept + "员工", "path", "/employee",
                            "query", createMap("deptId", highestDeptId))
            )));
            insights.add(insight);
        }

        // 洞察2: 高绩效员工识别
        long highPerfCount = performances.stream()
                .filter(p -> p.getGrade() != null && ("S".equals(p.getGrade()) || "A".equals(p.getGrade())))
                .count();
        long highPerfWithPotential = performances.stream()
                .filter(p -> p.getGrade() != null && "S".equals(p.getGrade()))
                .count();
        if (highPerfCount > 0) {
            AiInsight insight = new AiInsight();
            insight.setTitle("高绩效员工识别");
            insight.setType("success");
            // 原文案把「考核记录数」写成「名」，会被读成 103 名员工，此处按实际口径改为人次
            insight.setDescription(String.format("考核记录中 S/A 级共%d人次，其中 S 级%d人次，可作为重点培养对象。", highPerfCount, highPerfWithPotential));
            insight.setSuggestion("建议：1. 纳入人才储备池；2. 提供领导力培训；3. 给予特殊激励。");
            insight.setConfidence(0.90);
            insight.setCategory("performance");
            insight.setEvidence(new ArrayList<>(List.of(
                    createMap("label", "S/A 级考核记录", "value", highPerfCount + " 人次", "source", "sys_performance.grade"),
                    createMap("label", "其中 S 级", "value", highPerfWithPotential + " 人次", "source", "sys_performance.grade"),
                    createMap("label", "考核记录总数", "value", performances.size() + " 条", "source", "sys_performance")
            )));
            insight.setBasis("按考核记录计数，同一员工多次考核会重复计入，因此单位是「人次」而不是「人数」。");
            insight.setTargets(new ArrayList<>(List.of(
                    createMap("label", "查看绩效记录", "path", "/performance")
            )));
            insights.add(insight);
        }

        // 洞察3: 薪资结构分析
        if (!activeEmps.isEmpty() && activeEmps.stream().allMatch(e -> e.getSalary() != null)) {
            double avgSal = activeEmps.stream()
                    .mapToDouble(e -> e.getSalary() != null ? e.getSalary().doubleValue() : 0)
                    .average().orElse(0);
            long belowAvg = activeEmps.stream()
                    .filter(e -> e.getSalary() != null && e.getSalary().doubleValue() < avgSal * 0.8)
                    .count();
            if (belowAvg > 0) {
                AiInsight insight = new AiInsight();
                insight.setTitle("薪资竞争力分析");
                insight.setType("info");
                insight.setDescription(String.format("当前平均薪资%.0f元，有%d名员工薪资低于平均值的80%%，存在薪资偏低风险。", avgSal, belowAvg));
                insight.setSuggestion("建议：1. 评估薪资竞争力；2. 制定调薪计划；3. 关注核心岗位薪资水平。");
                insight.setConfidence(0.78);
                insight.setCategory("salary");
                insight.setEvidence(new ArrayList<>(List.of(
                        createMap("label", "在职员工平均薪资", "value", String.format("%,.0f 元", avgSal), "source", "sys_employee.salary"),
                        createMap("label", "低于平均值 80% 的员工", "value", belowAvg + " 人", "source", "个人薪资 < 平均薪资 × 80%"),
                        createMap("label", "在职员工数", "value", activeEmps.size() + " 人", "source", "sys_employee.status = 1")
                )));
                insight.setBasis("平均薪资按在职员工计算，不含离职员工，因此与薪资分析页的全体口径会有差异；"
                        + "「偏低」判定为个人薪资 < 在职平均薪资 × 80%。");
                insight.setTargets(new ArrayList<>(List.of(
                        createMap("label", "查看薪资明细", "path", "/salary")
                )));
                insights.add(insight);
            }
        }

        // 洞察4: 考勤异常关注
        if (!attendances.isEmpty()) {
            long abnormalCount = attendances.stream()
                    .filter(a -> a.getStatus() != null && (a.getStatus() == 2 || a.getStatus() == 3 || a.getStatus() == 4))
                    .count();
            double abnormalRate = round(abnormalCount * 100.0 / attendances.size(), 2);
            if (abnormalRate > 5) {
                AiInsight insight = new AiInsight();
                insight.setTitle("考勤异常关注");
                insight.setType("warning");
                insight.setDescription(String.format("近期考勤异常率为%.1f%%（迟到/早退/缺勤），建议关注员工工作状态。", abnormalRate));
                insight.setSuggestion("建议：1. 部门经理面谈了解情况；2. 评估工作安排是否合理；3. 提供必要支持。");
                insight.setConfidence(0.88);
                insight.setCategory("attendance");
                insight.setEvidence(new ArrayList<>(List.of(
                        createMap("label", "考勤记录总数", "value", attendances.size() + " 条", "source", "sys_attendance"),
                        createMap("label", "异常记录（迟到/早退/缺勤）", "value", abnormalCount + " 条", "source", "sys_attendance.status ∈ (2,3,4)"),
                        createMap("label", "异常率", "value", String.format("%.1f%%", abnormalRate), "source", "异常记录数 ÷ 记录总数")
                )));
                insight.setBasis("异常率的分母是全部考勤记录数，不是员工人数；一条记录对应一人一天，因此数值会比「异常员工占比」低。");
                insight.setTargets(new ArrayList<>(List.of(
                        createMap("label", "查看考勤记录", "path", "/attendance")
                )));
                insights.add(insight);
            }
        }

        // 洞察5: 人才结构分析
        long highEduCount = activeEmps.stream()
                .filter(e -> e.getEducation() != null && (e.getEducation().equals("博士") || e.getEducation().equals("硕士")))
                .count();
        double highEduRate = activeEmps.isEmpty() ? 0 : round(highEduCount * 100.0 / activeEmps.size(), 2);
        double avgAge = activeEmps.stream()
                .filter(e -> e.getBirthday() != null)
                .mapToInt(e -> LocalDate.now().getYear() - e.getBirthday().getYear())
                .average().orElse(0);
        if (avgAge > 0) {
            AiInsight insight = new AiInsight();
            insight.setTitle("人才结构优化");
            insight.setType("info");
            // 原口径只统计博士 + 硕士，却写成「本科及以上」，会把 17.4% 误读成全员学历水平
            insight.setDescription(String.format("硕士及以上学历占比%.1f%%（%d/%d 人），平均年龄%.1f岁，团队%s。",
                    highEduRate, highEduCount, activeEmps.size(), avgAge,
                    avgAge < 30 ? "呈年轻化趋势，创新活力较强" : "年龄结构均衡，经验积累丰富"));
            insight.setSuggestion("建议：1. 保持学历结构健康；2. 关注年龄多样性；3. 建立梯队培养机制。");
            insight.setConfidence(0.82);
            insight.setCategory("structure");
            insight.setEvidence(new ArrayList<>(List.of(
                    createMap("label", "在职员工数", "value", activeEmps.size() + " 人", "source", "sys_employee.status = 1"),
                    createMap("label", "硕士及以上", "value", highEduCount + " 人", "source", "sys_employee.education"),
                    createMap("label", "平均年龄", "value", String.format("%.1f 岁", avgAge), "source", "sys_employee.birthday")
            )));
            insight.setBasis("学历占比按在职员工统计，未填学历的员工不计入；"
                    + "平均年龄由生日推算，未填生日的员工不计入，因此分母可能小于在职人数。");
            insight.setTargets(new ArrayList<>(List.of(
                    createMap("label", "查看员工列表", "path", "/employee")
            )));
            insights.add(insight);
        }

        return insights;
    }

    // ==================== 首页最新动态（真实数据） ====================

    /** 首页动态的内部排序结构 */
    private static class Activity {
        final String type;
        final String content;
        final LocalDateTime at;

        Activity(String type, String content, LocalDateTime at) {
            this.type = type;
            this.content = content;
            this.at = at;
        }
    }

    @Override
    public List<Map<String, Object>> getRecentActivities(int limit) {
        int cap = Math.max(1, Math.min(limit, 20));
        LocalDate today = LocalDate.now();
        Map<Long, String> deptNames = departmentMapper.selectList(null).stream()
                .filter(d -> d.getId() != null && d.getDeptName() != null)
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));

        List<Activity> items = new ArrayList<>();

        // 入职：只取已经发生的日期，未来的入职属于「待入职」，不能算成已入职
        try {
            employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                            .isNotNull(Employee::getEntryDate)
                            .le(Employee::getEntryDate, today)
                            .ge(Employee::getEntryDate, today.minusDays(90))
                            .orderByDesc(Employee::getEntryDate)
                            .last("LIMIT 8"))
                    .forEach(e -> {
                        String dept = deptNames.get(e.getDeptId());
                        items.add(new Activity("onboard",
                                e.getName() + " 入职" + (dept == null ? "" : "，分配至" + dept),
                                e.getEntryDate().atStartOfDay()));
                    });
        } catch (Exception ex) {
            log.warn("取最近入职失败：{}", ex.getMessage());
        }

        // 离职：近 180 天
        try {
            employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                            .isNotNull(Employee::getLeaveDate)
                            .le(Employee::getLeaveDate, today)
                            .ge(Employee::getLeaveDate, today.minusDays(180))
                            .orderByDesc(Employee::getLeaveDate)
                            .last("LIMIT 8"))
                    .forEach(e -> items.add(new Activity("leave",
                            e.getName() + " 离职",
                            e.getLeaveDate().atStartOfDay())));
        } catch (Exception ex) {
            log.warn("取最近离职失败：{}", ex.getMessage());
        }

        // 审批：近 30 天，title 本身已含申请人，不再重复拼接姓名。
        // 限 4 条：审批往往一天多条，不设上限会把人员变动挤出首页
        try {
            approvalInstanceMapper.selectList(new LambdaQueryWrapper<ApprovalInstance>()
                            .ge(ApprovalInstance::getUpdateTime, LocalDateTime.now().minusDays(30))
                            .orderByDesc(ApprovalInstance::getUpdateTime)
                            .last("LIMIT 4"))
                    .forEach(a -> items.add(new Activity(approvalType(a.getStatus()),
                            a.getTitle() + " · " + approvalStatusText(a.getStatus()),
                            a.getUpdateTime())));
        } catch (Exception ex) {
            log.warn("取最近审批失败：{}", ex.getMessage());
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return items.stream()
                .filter(a -> a.at != null)
                .sorted(Comparator.comparing((Activity a) -> a.at).reversed())
                .limit(cap)
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", a.type);
                    m.put("content", a.content);
                    m.put("time", humanize(a.at));
                    m.put("at", a.at.format(fmt));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private String approvalType(Integer status) {
        if (status == null) {
            return "pending";
        }
        return switch (status) {
            case 1 -> "approved";
            case 2 -> "rejected";
            default -> "pending";
        };
    }

    private String approvalStatusText(Integer status) {
        if (status == null) {
            return "待审批";
        }
        return switch (status) {
            case 1 -> "已通过";
            case 2 -> "已驳回";
            default -> "待审批";
        };
    }

    /** 相对时间：首页看的是「多久之前」，超过一周才退回日期 */
    private String humanize(LocalDateTime at) {
        Duration d = Duration.between(at, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes <= 0) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + " 分钟前";
        }
        long hours = d.toHours();
        if (hours < 24) {
            return hours + " 小时前";
        }
        long days = d.toDays();
        if (days == 1) {
            return "昨天";
        }
        if (days < 7) {
            return days + " 天前";
        }
        return at.format(DateTimeFormatter.ofPattern("MM-dd"));
    }

    @Override
    public RecruitmentAnalysis getRecruitmentAnalysis() {
        RecruitmentAnalysis analysis = new RecruitmentAnalysis();
        analysis.setTotalPositions(12);
        analysis.setTotalHeadcount(25);
        analysis.setFilledPositions(8);
        analysis.setInProgress(4);

        // 部门需求分布
        List<Map<String, Object>> deptDemand = new ArrayList<>();
        deptDemand.add(createMap("name", "技术部", "value", 12));
        deptDemand.add(createMap("name", "市场部", "value", 5));
        deptDemand.add(createMap("name", "人事部", "value", 2));
        deptDemand.add(createMap("name", "财务部", "value", 3));
        deptDemand.add(createMap("name", "行政部", "value", 3));
        analysis.setDeptDemand(deptDemand);

        // 职位需求分布
        List<Map<String, Object>> posDemand = new ArrayList<>();
        posDemand.add(createMap("name", "开发工程师", "value", 8));
        posDemand.add(createMap("name", "测试工程师", "value", 3));
        posDemand.add(createMap("name", "产品经理", "value", 2));
        posDemand.add(createMap("name", "销售", "value", 5));
        posDemand.add(createMap("name", "其他", "value", 7));
        analysis.setPositionDemand(posDemand);

        // 招聘周期趋势
        List<Map<String, Object>> timeTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("month", date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            map.put("avgDays", 25 + Math.random() * 15);
            map.put("hiredCount", (int)(3 + Math.random() * 5));
            timeTrend.add(map);
        }
        analysis.setTimeToHireTrend(timeTrend);

        return analysis;
    }

    @Override
    public TrainingAnalysis getTrainingAnalysis() {
        TrainingAnalysis analysis = new TrainingAnalysis();
        analysis.setTotalTrainings(24);
        analysis.setCompletedTrainings(18);
        analysis.setTotalParticipants(320);
        analysis.setAvgScore(82.5);

        // 培训类型分布
        List<Map<String, Object>> typeDist = new ArrayList<>();
        typeDist.add(createMap("name", "技术培训", "value", 10));
        typeDist.add(createMap("name", "管理培训", "value", 5));
        typeDist.add(createMap("name", "入职培训", "value", 4));
        typeDist.add(createMap("name", "软技能培训", "value", 3));
        typeDist.add(createMap("name", "安全培训", "value", 2));
        analysis.setTypeDistribution(typeDist);

        // 部门参与度
        List<Map<String, Object>> deptParticipation = new ArrayList<>();
        deptParticipation.add(createMap("name", "技术部", "participationRate", 85.5, "avgScore", 84.2));
        deptParticipation.add(createMap("name", "市场部", "participationRate", 72.3, "avgScore", 80.1));
        deptParticipation.add(createMap("name", "人事部", "participationRate", 90.0, "avgScore", 88.5));
        deptParticipation.add(createMap("name", "财务部", "participationRate", 68.5, "avgScore", 79.3));
        deptParticipation.add(createMap("name", "行政部", "participationRate", 75.0, "avgScore", 81.6));
        analysis.setDeptParticipation(deptParticipation);

        // 培训效果
        List<Map<String, Object>> effect = new ArrayList<>();
        effect.add(createMap("name", "知识掌握度", "score", 85.0));
        effect.add(createMap("name", "技能提升度", "score", 78.5));
        effect.add(createMap("name", "工作应用度", "score", 72.0));
        effect.add(createMap("name", "满意度", "score", 88.5));
        analysis.setTrainingEffect(effect);

        return analysis;
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> createMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i].toString(), keyValues[i + 1]);
        }
        return map;
    }

    private List<Map<String, Object>> convertMapToList(Map<String, Long> map) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", entry.getKey());
            m.put("value", entry.getValue());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> sortAndConvertMap(Map<String, Long> map, List<String> order) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String key : order) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", key);
            m.put("value", map.getOrDefault(key, 0L));
            list.add(m);
        }
        // 添加不在顺序列表中的
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            if (!order.contains(entry.getKey())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", entry.getKey());
                m.put("value", entry.getValue());
                list.add(m);
            }
        }
        return list;
    }

    private double round(double value, int places) {
        return Math.round(value * Math.pow(10, places)) / Math.pow(10, places);
    }

    private double calculateAvg(List<Performance> list, String type) {
        if (list == null || list.isEmpty()) return 0;
        return list.stream()
                .mapToDouble(p -> {
                    switch (type) {
                        case "quality": return p.getWorkQuality() != null ? p.getWorkQuality().doubleValue() : 0;
                        case "efficiency": return p.getWorkEfficiency() != null ? p.getWorkEfficiency().doubleValue() : 0;
                        case "attitude": return p.getWorkAttitude() != null ? p.getWorkAttitude().doubleValue() : 0;
                        case "teamwork": return p.getTeamwork() != null ? p.getTeamwork().doubleValue() : 0;
                        case "innovation": return p.getInnovation() != null ? p.getInnovation().doubleValue() : 0;
                        default: return 0;
                    }
                })
                .average()
                .orElse(0);
    }
}
