package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Attendance;
import com.hrms.entity.Employee;
import com.hrms.entity.Overtime;
import com.hrms.entity.Performance;
import com.hrms.entity.Salary;
import com.hrms.mapper.AttendanceMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.OvertimeMapper;
import com.hrms.mapper.PerformanceMapper;
import com.hrms.mapper.SalaryMapper;
import com.hrms.service.SalaryService;
import com.hrms.util.AttendanceStatus;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 薪资Service实现类
 */
@Service
public class SalaryServiceImpl implements SalaryService {

    @Autowired
    private SalaryMapper salaryMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private PerformanceMapper performanceMapper;

    @Autowired
    private OvertimeMapper overtimeMapper;

    @Autowired
    private com.hrms.service.SysConfigService sysConfigService;

    @Override
    public Salary getById(Long id) {
        return salaryMapper.selectById(id);
    }

    @Override
    public List<Salary> listByEmpId(Long empId) {
        LambdaQueryWrapper<Salary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Salary::getEmpId, empId)
                .orderByDesc(Salary::getSalaryMonth);
        return salaryMapper.selectList(queryWrapper);
    }

    @Override
    public List<Salary> listByMonth(String salaryMonth) {
        LambdaQueryWrapper<Salary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Salary::getSalaryMonth, salaryMonth)
                .orderByAsc(Salary::getEmpId);
        List<Salary> list = salaryMapper.selectList(queryWrapper);
        // P4-3 部门数据权限：部门经理仅看本部门员工薪资
        if (UserContext.isManager() && UserContext.getDeptId() != null) {
            Set<Long> deptEmpIds = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                            .eq(Employee::getDeptId, UserContext.getDeptId()))
                    .stream().map(Employee::getId).collect(Collectors.toSet());
            list = list.stream().filter(s -> deptEmpIds.contains(s.getEmpId())).collect(Collectors.toList());
        }
        return list;
    }

    @Override
    public Map<String, Object> monthlyStatistics(String salaryMonth) {
        List<Salary> list = listByMonth(salaryMonth);

        Map<String, Object> result = new HashMap<>();
        BigDecimal totalBaseSalary = BigDecimal.ZERO;
        BigDecimal totalPerformanceBonus = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalAllowance = BigDecimal.ZERO;
        BigDecimal totalSocialSecurity = BigDecimal.ZERO;
        BigDecimal totalHousingFund = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalOtherDeduction = BigDecimal.ZERO;
        BigDecimal totalNetSalary = BigDecimal.ZERO;

        for (Salary salary : list) {
            if (salary.getBaseSalary() != null) {
                totalBaseSalary = totalBaseSalary.add(salary.getBaseSalary());
            }
            if (salary.getPerformanceBonus() != null) {
                totalPerformanceBonus = totalPerformanceBonus.add(salary.getPerformanceBonus());
            }
            if (salary.getOvertimePay() != null) {
                totalOvertimePay = totalOvertimePay.add(salary.getOvertimePay());
            }
            if (salary.getAllowance() != null) {
                totalAllowance = totalAllowance.add(salary.getAllowance());
            }
            if (salary.getSocialSecurity() != null) {
                totalSocialSecurity = totalSocialSecurity.add(salary.getSocialSecurity());
            }
            if (salary.getHousingFund() != null) {
                totalHousingFund = totalHousingFund.add(salary.getHousingFund());
            }
            if (salary.getTax() != null) {
                totalTax = totalTax.add(salary.getTax());
            }
            if (salary.getOtherDeduction() != null) {
                totalOtherDeduction = totalOtherDeduction.add(salary.getOtherDeduction());
            }
            if (salary.getNetSalary() != null) {
                totalNetSalary = totalNetSalary.add(salary.getNetSalary());
            }
        }

        result.put("employeeCount", list.size());
        result.put("totalBaseSalary", totalBaseSalary.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalPerformanceBonus", totalPerformanceBonus.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalOvertimePay", totalOvertimePay.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalAllowance", totalAllowance.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalSocialSecurity", totalSocialSecurity.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalHousingFund", totalHousingFund.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalTax", totalTax.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalOtherDeduction", totalOtherDeduction.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalNetSalary", totalNetSalary.setScale(2, BigDecimal.ROUND_HALF_UP));

        return result;
    }

    @Override
    public boolean save(Salary salary) {
        // P042：同员工同月防重（逻辑删除的记录不算），避免裸 SQL 唯一键报错
        Long empId = salary.getEmpId();
        String month = salary.getSalaryMonth();
        if (empId != null && month != null) {
            LambdaQueryWrapper<Salary> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(Salary::getEmpId, empId).eq(Salary::getSalaryMonth, month);
            if (salaryMapper.selectCount(dupWrapper) > 0) {
                throw new RuntimeException("该员工本月薪资记录已存在，请直接编辑原记录");
            }
        }
        // 自动计算实发工资
        calculateNetSalary(salary);
        salary.setCreateTime(LocalDateTime.now());
        salary.setUpdateTime(LocalDateTime.now());
        return salaryMapper.insert(salary) > 0;
    }

    @Override
    public boolean update(Salary salary) {
        calculateNetSalary(salary);
        salary.setUpdateTime(LocalDateTime.now());
        return salaryMapper.updateById(salary) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return salaryMapper.deleteById(id) > 0;
    }

    /** 以下常量仅作兜底默认值，实际取值以 sys_config 参数为准（P5-3 参数可配） */
    private static final BigDecimal DEFAULT_LATE_EARLY_FINE = new BigDecimal("50");
    private static final BigDecimal DEFAULT_SOCIAL_SECURITY_RATE = new BigDecimal("0.105");
    private static final BigDecimal DEFAULT_HOUSING_FUND_RATE = new BigDecimal("0.07");
    private static final BigDecimal DEFAULT_PERFORMANCE_MAX_RATE = new BigDecimal("0.2");
    private static final BigDecimal DEFAULT_PERFORMANCE_FULL_SCORE = new BigDecimal("500");
    private static final BigDecimal DEFAULT_TAX_THRESHOLD = new BigDecimal("5000");
    private static final BigDecimal DEFAULT_WORK_DAYS_BASE = new BigDecimal("21.75");
    private static final BigDecimal DEFAULT_OT_WEEKDAY_RATE = new BigDecimal("1.5");
    private static final BigDecimal DEFAULT_OT_WEEKEND_RATE = new BigDecimal("2.0");

    @Override
    public Map<String, Object> calculateMonth(String salaryMonth) {
        // P5-3：算薪参数从 sys_config 读取（循环外取一次，失败回退默认值）
        BigDecimal lateEarlyFine = sysConfigService.getDecimal("salary.lateEarlyFine", DEFAULT_LATE_EARLY_FINE);
        BigDecimal socialSecurityRate = sysConfigService.getDecimal("salary.socialSecurityRate", DEFAULT_SOCIAL_SECURITY_RATE);
        BigDecimal housingFundRate = sysConfigService.getDecimal("salary.housingFundRate", DEFAULT_HOUSING_FUND_RATE);
        BigDecimal performanceMaxRate = sysConfigService.getDecimal("salary.performanceMaxRate", DEFAULT_PERFORMANCE_MAX_RATE);
        BigDecimal performanceFullScore = sysConfigService.getDecimal("salary.performanceFullScore", DEFAULT_PERFORMANCE_FULL_SCORE);
        BigDecimal taxThreshold = sysConfigService.getDecimal("salary.taxThreshold", DEFAULT_TAX_THRESHOLD);
        BigDecimal workDaysBase = sysConfigService.getDecimal("salary.workDaysBase", DEFAULT_WORK_DAYS_BASE);
        BigDecimal otWeekdayRate = sysConfigService.getDecimal("attendance.otWeekdayRate", DEFAULT_OT_WEEKDAY_RATE);
        BigDecimal otWeekendRate = sysConfigService.getDecimal("attendance.otWeekendRate", DEFAULT_OT_WEEKEND_RATE);

        YearMonth yearMonth = YearMonth.parse(salaryMonth);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();
        // 当月未结束只按已过工作日算缺勤，避免月末未到全员"缺勤"
        LocalDate attendDeadline = yearMonth.equals(YearMonth.from(today)) ? today : endDate;

        // 在职员工（status 0 为离职）
        List<Employee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>().ne(Employee::getStatus, 0).orderByAsc(Employee::getId));

        int created = 0;
        int skipped = 0;
        for (Employee emp : employees) {
            LambdaQueryWrapper<Salary> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(Salary::getEmpId, emp.getId()).eq(Salary::getSalaryMonth, salaryMonth);
            if (salaryMapper.selectCount(dupWrapper) > 0) {
                skipped++;
                continue;
            }

            Salary salary = new Salary();
            salary.setEmpId(emp.getId());
            salary.setSalaryMonth(salaryMonth);

            // 1) 底薪取员工档案
            BigDecimal base = emp.getSalary() == null ? BigDecimal.ZERO : emp.getSalary();
            salary.setBaseSalary(base);

            // 2) 考勤联动：缺勤扣款（日薪 × 缺勤天数）+ 迟到早退扣款
            long workDays = countWorkDays(startDate, attendDeadline);
            List<Attendance> attList = attendanceMapper.selectList(
                    new LambdaQueryWrapper<Attendance>()
                            .eq(Attendance::getEmpId, emp.getId())
                            .between(Attendance::getAttendDate, startDate, endDate));
            long attendDays = 0;
            long leaveDays = 0;
            int lateCount = 0;
            int earlyCount = 0;
            for (Attendance att : attList) {
                Integer st = att.getStatus();
                if (AttendanceStatus.isLate(st)) {
                    lateCount++;
                }
                if (AttendanceStatus.isEarlyLeave(st)) {
                    earlyCount++;
                }
                if (AttendanceStatus.isLeave(st)) {
                    leaveDays++;
                } else {
                    attendDays++;
                }
            }
            // 请假已审批通过的天数不算缺勤，否则会出现「扣年假额度 + 扣工资」双重惩罚
            long absentDays = Math.max(0, workDays - attendDays - leaveDays);
            BigDecimal daySalary = base.divide(workDaysBase, 4, RoundingMode.HALF_UP);
            BigDecimal absentCut = daySalary.multiply(BigDecimal.valueOf(absentDays))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal lateCut = lateEarlyFine.multiply(BigDecimal.valueOf(lateCount + earlyCount));
            salary.setOtherDeduction(absentCut.add(lateCut));

            // 3) 绩效联动：当月评分取最高分（sys_performance.status 语义未统一，这里不做状态过滤）
            List<Performance> perfList = performanceMapper.selectList(
                    new LambdaQueryWrapper<Performance>()
                            .eq(Performance::getEmpId, emp.getId())
                            .eq(Performance::getPeriod, salaryMonth)
                            .orderByDesc(Performance::getTotalScore));
            Performance best = perfList.isEmpty() ? null : perfList.get(0);
            BigDecimal bonus = BigDecimal.ZERO;
            if (best != null && best.getTotalScore() != null) {
                BigDecimal rate = best.getTotalScore().min(performanceFullScore)
                        .divide(performanceFullScore, 4, RoundingMode.HALF_UP);
                bonus = base.multiply(performanceMaxRate).multiply(rate).setScale(2, RoundingMode.HALF_UP);
            }
            salary.setPerformanceBonus(bonus);

            // 4) 加班费联动（P4-1）：当月已通过的加班单，工作日 1.5 倍、周末 2.0 倍时薪
            BigDecimal overtimePay = BigDecimal.ZERO;
            BigDecimal totalOtHours = BigDecimal.ZERO;
            List<Overtime> otList = overtimeMapper.selectList(new LambdaQueryWrapper<Overtime>()
                    .eq(Overtime::getEmpId, emp.getId())
                    .eq(Overtime::getStatus, 1)
                    .between(Overtime::getOvertimeDate, startDate, endDate));
            if (!otList.isEmpty()) {
                BigDecimal hourWage = daySalary.divide(new BigDecimal("8"), 4, RoundingMode.HALF_UP);
                for (Overtime ot : otList) {
                    if (ot.getHours() == null) {
                        continue;
                    }
                    DayOfWeek w = ot.getOvertimeDate() == null ? DayOfWeek.MONDAY : ot.getOvertimeDate().getDayOfWeek();
                    BigDecimal rate = (w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY)
                            ? otWeekendRate : otWeekdayRate;
                    overtimePay = overtimePay.add(hourWage.multiply(rate).multiply(ot.getHours()));
                    totalOtHours = totalOtHours.add(ot.getHours());
                }
            }
            salary.setOvertimePay(overtimePay.setScale(2, RoundingMode.HALF_UP));
            salary.setAllowance(BigDecimal.ZERO);

            // 5) 五险一金按比例估算
            salary.setSocialSecurity(base.multiply(socialSecurityRate).setScale(2, RoundingMode.HALF_UP));
            salary.setHousingFund(base.multiply(housingFundRate).setScale(2, RoundingMode.HALF_UP));

            // 6) 简化月度个税：（应发 − 起征点 − 社保公积金）按月度税率表
            BigDecimal taxable = base.add(bonus).add(overtimePay).subtract(taxThreshold)
                    .subtract(salary.getSocialSecurity()).subtract(salary.getHousingFund());
            salary.setTax(monthlyTax(taxable.max(BigDecimal.ZERO)));

            // 7) 实发 = 应发 − 各项扣除（复用既有逻辑）；保底实发不为负（当月无考勤的新员工避免扣穿）
            calculateNetSalary(salary);
            if (salary.getNetSalary().compareTo(BigDecimal.ZERO) < 0) {
                salary.setOtherDeduction(salary.getOtherDeduction().add(salary.getNetSalary()));
                calculateNetSalary(salary);
            }

            String perfText = best != null && best.getTotalScore() != null
                    ? best.getTotalScore().stripTrailingZeros().toPlainString() + "分" : "无";
            String otText = totalOtHours.compareTo(BigDecimal.ZERO) > 0
                    ? String.format("；加班%.1fh(¥%s)", totalOtHours, overtimePay.setScale(2, RoundingMode.HALF_UP).toPlainString())
                    : "";
            salary.setRemark(String.format(
                    "系统计算：出勤%d天/缺勤%d天/迟到早退%d次；绩效%s；缺勤扣%.2f元%s",
                    attendDays, absentDays, lateCount + earlyCount, perfText, absentCut, otText));
            salary.setStatus(1);
            salary.setCreateTime(LocalDateTime.now());
            salary.setUpdateTime(LocalDateTime.now());
            salaryMapper.insert(salary);
            created++;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("month", salaryMonth);
        data.put("total", employees.size());
        data.put("created", created);
        data.put("skipped", skipped);
        return data;
    }

    /**
     * 统计 [start, end] 内的工作日（周一至周五）
     */
    private long countWorkDays(LocalDate start, LocalDate end) {
        long count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    /**
     * 简化个税：按月度税率表速算（不考虑专项附加扣除）
     */
    private BigDecimal monthlyTax(BigDecimal taxable) {
        BigDecimal[] brackets = {
                new BigDecimal("3000"), new BigDecimal("12000"), new BigDecimal("25000"),
                new BigDecimal("35000"), new BigDecimal("55000"), new BigDecimal("80000")
        };
        BigDecimal[] rates = {
                new BigDecimal("0.03"), new BigDecimal("0.10"), new BigDecimal("0.20"),
                new BigDecimal("0.25"), new BigDecimal("0.30"), new BigDecimal("0.35"), new BigDecimal("0.45")
        };
        BigDecimal[] deductions = {
                BigDecimal.ZERO, new BigDecimal("210"), new BigDecimal("1410"),
                new BigDecimal("2660"), new BigDecimal("4410"), new BigDecimal("7160"), new BigDecimal("15160")
        };
        int level = brackets.length;
        for (int i = 0; i < brackets.length; i++) {
            if (taxable.compareTo(brackets[i]) <= 0) {
                level = i;
                break;
            }
        }
        return taxable.multiply(rates[level]).subtract(deductions[level])
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算实发工资
     */
    private void calculateNetSalary(Salary salary) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalDeduction = BigDecimal.ZERO;

        if (salary.getBaseSalary() != null) {
            totalIncome = totalIncome.add(salary.getBaseSalary());
        }
        if (salary.getPerformanceBonus() != null) {
            totalIncome = totalIncome.add(salary.getPerformanceBonus());
        }
        if (salary.getOvertimePay() != null) {
            totalIncome = totalIncome.add(salary.getOvertimePay());
        }
        if (salary.getAllowance() != null) {
            totalIncome = totalIncome.add(salary.getAllowance());
        }
        if (salary.getSocialSecurity() != null) {
            totalDeduction = totalDeduction.add(salary.getSocialSecurity());
        }
        if (salary.getHousingFund() != null) {
            totalDeduction = totalDeduction.add(salary.getHousingFund());
        }
        if (salary.getTax() != null) {
            totalDeduction = totalDeduction.add(salary.getTax());
        }
        if (salary.getOtherDeduction() != null) {
            totalDeduction = totalDeduction.add(salary.getOtherDeduction());
        }

        salary.setNetSalary(totalIncome.subtract(totalDeduction).setScale(2, BigDecimal.ROUND_HALF_UP));
    }
}
