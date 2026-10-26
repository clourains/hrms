package com.hrms.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Attendance;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.mapper.AttendanceMapper;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考勤月报导出 Controller（P1-3）
 *
 * 单独拆出，避免在 AttendanceController 里混入 Excel 依赖。
 * Sheet1 月度汇总（每员工一行：出勤/迟到/早退/工时），Sheet2 打卡明细。
 */
@RestController
@RequestMapping("/attendance")
public class AttendanceExportController {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    /**
     * 导出考勤月报 Excel
     *
     * @param month  yyyy-MM，必填
     * @param deptId 可选，按部门过滤导出范围
     */
    @RequireRole("admin")
    @GetMapping("/export")
    public void export(@RequestParam String month,
                       @RequestParam(required = false) Long deptId,
                       HttpServletResponse response) throws Exception {
        // 校验月份
        YearMonth ym;
        try {
            ym = YearMonth.parse(month, MONTH_FMT);
        } catch (Exception e) {
            response.setStatus(400);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":400,\"message\":\"月份格式应为 yyyy-MM\",\"data\":null}");
            return;
        }
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        // 员工与部门映射（deptId 可选过滤）
        List<Employee> employees = employeeMapper.selectList(null).stream()
                .filter(e -> deptId == null || deptId.equals(e.getDeptId()))
                .toList();
        Map<Long, Employee> empMap = employees.stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        List<Long> deptIds = employees.stream()
                .map(Employee::getDeptId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> deptMap = deptIds.isEmpty() ? Collections.emptyMap()
                : departmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        // 该月考勤记录（限导出范围内的员工）
        Set<Long> empIdFilter = empMap.keySet();
        List<Attendance> records = attendanceMapper.selectList(
                        new LambdaQueryWrapper<Attendance>()
                                .between(Attendance::getAttendDate, monthStart, monthEnd))
                .stream()
                .filter(a -> empIdFilter.contains(a.getEmpId()))
                .toList();
        Map<Long, List<Attendance>> byEmp = records.stream()
                .collect(Collectors.groupingBy(Attendance::getEmpId));

        ExcelWriter writer = ExcelUtil.getBigWriter();
        // Sheet1 月度汇总
        writer.renameSheet("月度汇总");
        writer.writeHeadRow(Arrays.asList("工号", "姓名", "部门", "出勤天数", "迟到次数", "早退次数", "异常(缺下班卡)", "总工时(h)"));
        List<Employee> sortedEmps = employees.stream()
                .sorted(Comparator.comparing(e -> e.getEmpNo() == null ? "" : e.getEmpNo()))
                .toList();
        for (Employee emp : sortedEmps) {
            List<Attendance> list = byEmp.getOrDefault(emp.getId(), Collections.emptyList());
            long attendDays = list.stream().filter(a -> a.getCheckInTime() != null).count();
            long lateCount = list.stream().filter(a -> a.getStatus() != null && (a.getStatus() == 1 || a.getStatus() == 3)).count();
            long earlyCount = list.stream().filter(a -> a.getStatus() != null && (a.getStatus() == 2 || a.getStatus() == 3)).count();
            long abnormal = list.stream().filter(a -> a.getCheckInTime() != null && a.getCheckOutTime() == null).count();
            double totalHours = list.stream()
                    .map(a -> a.getWorkHours())
                    .filter(Objects::nonNull)
                    .mapToDouble(java.math.BigDecimal::doubleValue)
                    .sum();
            writer.writeRow(Arrays.asList(
                    emp.getEmpNo(),
                    emp.getName(),
                    emp.getDeptId() != null ? deptMap.get(emp.getDeptId()) : "",
                    attendDays, lateCount, earlyCount, abnormal,
                    Math.round(totalHours * 10) / 10.0));
        }

        // Sheet2 打卡明细
        writer.setSheet("打卡明细");
        writer.writeHeadRow(Arrays.asList("工号", "姓名", "部门", "考勤日期", "上班时间", "下班时间", "状态", "工时(h)", "备注"));
        List<Attendance> sorted = records.stream()
                .sorted(Comparator.comparing(Attendance::getAttendDate)
                        .thenComparing(a -> a.getEmpId() == null ? Long.MAX_VALUE : a.getEmpId()))
                .toList();
        for (Attendance a : sorted) {
            Employee emp = empMap.get(a.getEmpId());
            writer.writeRow(Arrays.asList(
                    emp != null ? emp.getEmpNo() : "",
                    emp != null ? emp.getName() : "未知(" + a.getEmpId() + ")",
                    emp != null && emp.getDeptId() != null ? deptMap.get(emp.getDeptId()) : "",
                    a.getAttendDate() != null ? a.getAttendDate().toString() : "",
                    a.getCheckInTime() != null ? TIME_FMT.format(a.getCheckInTime()) : "",
                    a.getCheckOutTime() != null ? TIME_FMT.format(a.getCheckOutTime()) : "",
                    statusText(a.getStatus()),
                    a.getWorkHours() != null ? a.getWorkHours().doubleValue() : "",
                    a.getRemark() != null ? a.getRemark() : ""));
        }

        String filename = URLEncoder.encode("考勤月报_" + month + ".xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + filename);
        try {
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    private String statusText(Integer status) {
        if (status == null) return "未打卡";
        return switch (status) {
            case 0 -> "正常";
            case 1 -> "迟到";
            case 2 -> "早退";
            case 3 -> "迟到+早退";
            default -> "未知(" + status + ")";
        };
    }
}
