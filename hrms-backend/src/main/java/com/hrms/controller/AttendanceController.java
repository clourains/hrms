package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Attendance;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.entity.LeaveQuota;
import com.hrms.entity.Overtime;
import com.hrms.entity.User;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.LeaveQuotaMapper;
import com.hrms.mapper.OvertimeMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.service.ApprovalService;
import com.hrms.service.AttendanceService;
import com.hrms.service.LeaveQuotaService;
import com.hrms.util.JwtUtil;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考勤Controller
 */
@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private OvertimeMapper overtimeMapper;

    @Autowired
    private LeaveQuotaMapper leaveQuotaMapper;

    @Autowired
    private LeaveQuotaService leaveQuotaService;

    @Autowired
    private ApprovalService approvalService;

    /**
     * 上班打卡
     */
    @PostMapping("/checkIn")
    public Result<Attendance> checkIn(HttpServletRequest request) {
        try {
            Long empId = getCurrentEmpId(request);
            Attendance attendance = attendanceService.checkIn(empId);
            return Result.success("打卡成功", attendance);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下班打卡
     */
    @PostMapping("/checkOut")
    public Result<Attendance> checkOut(HttpServletRequest request) {
        try {
            Long empId = getCurrentEmpId(request);
            Attendance attendance = attendanceService.checkOut(empId);
            return Result.success("打卡成功", attendance);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID获取考勤记录
     */
    @GetMapping("/{id}")
    public Result<Attendance> getById(@PathVariable Long id) {
        try {
            Attendance attendance = attendanceService.getById(id);
            return Result.success(attendance);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取员工月度考勤列表（P4-3：manager 仅可查本部门员工）
     */
    @GetMapping("/list")
    public Result<List<Attendance>> list(@RequestParam Long empId, @RequestParam String month) {
        try {
            checkDeptScope(empId);
            List<Attendance> list = attendanceService.listByEmpIdAndMonth(empId, month);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 月度考勤统计（P4-3：manager 仅可查本部门员工）
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam Long empId, @RequestParam String month) {
        try {
            checkDeptScope(empId);
            Map<String, Object> statistics = attendanceService.monthlyStatistics(empId, month);
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增考勤记录
     */
    @RequireRole("admin")
    @PostMapping
    public Result<Void> save(@RequestBody Attendance attendance) {
        try {
            boolean result = attendanceService.save(attendance);
            if (result) {
                return Result.success("新增成功", null);
            } else {
                return Result.error("新增失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新考勤记录
     */
    @RequireRole("admin")
    @PutMapping
    public Result<Void> update(@RequestBody Attendance attendance) {
        try {
            boolean result = attendanceService.update(attendance);
            if (result) {
                return Result.success("更新成功", null);
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID删除考勤记录
     */
    @RequireRole("admin")
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            boolean result = attendanceService.deleteById(id);
            if (result) {
                return Result.success("删除成功", null);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== P4-1 加班申请 ====================

    /**
     * 提交加班申请：创建加班单 + 发起通用审批（bizType=加班），全部通过后计入薪资加班费
     */
    @AuditLog(module = "考勤管理", action = "提交加班申请")
    @PostMapping("/overtime/apply")
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> applyOvertime(@RequestBody Map<String, Object> body) {
        try {
            Long empId = getCurrentEmpId(null);
            Object dateObj = body.get("overtimeDate");
            Object hoursObj = body.get("hours");
            if (dateObj == null || Objects.toString(dateObj).isEmpty()) {
                return Result.error("请选择加班日期");
            }
            LocalDate overtimeDate;
            try {
                overtimeDate = LocalDate.parse(Objects.toString(dateObj).substring(0, 10));
            } catch (Exception e) {
                return Result.error("加班日期格式不正确");
            }
            BigDecimal hours;
            try {
                hours = new BigDecimal(Objects.toString(hoursObj, "0"));
            } catch (Exception e) {
                return Result.error("加班时长格式不正确");
            }
            if (hours.compareTo(new BigDecimal("0.5")) < 0 || hours.compareTo(new BigDecimal("24")) > 0) {
                return Result.error("加班时长须在 0.5 ~ 24 小时之间");
            }
            String reason = Objects.toString(body.get("reason"), "").trim();
            Object approversObj = body.get("approvers");
            if (!(approversObj instanceof List<?>) || ((List<?>) approversObj).isEmpty()) {
                return Result.error("请选择审批人");
            }

            Employee emp = employeeMapper.selectById(empId);
            String empName = emp != null && emp.getName() != null ? emp.getName() : "员工" + empId;

            // 1) 落加班单（审批中）
            Overtime ot = new Overtime();
            ot.setEmpId(empId);
            ot.setOvertimeDate(overtimeDate);
            ot.setHours(hours);
            ot.setReason(reason);
            ot.setStatus(0);
            ot.setCreateTime(java.time.LocalDateTime.now());
            ot.setUpdateTime(java.time.LocalDateTime.now());
            overtimeMapper.insert(ot);

            // 2) 发起审批单（复用通用审批流），失败抛异常回滚加班单
            String me = UserContext.get();
            Map<String, Object> applyBody = new HashMap<>();
            applyBody.put("bizType", "加班");
            applyBody.put("title", empName + " " + overtimeDate + " 加班 " + hours.stripTrailingZeros().toPlainString() + "小时");
            Map<String, Object> detail = new HashMap<>();
            detail.put("overtimeDate", overtimeDate.toString());
            detail.put("hours", hours);
            detail.put("reason", reason);
            applyBody.put("detail", detail);
            applyBody.put("approvers", approversObj);
            Result<Long> applyResult = approvalService.apply(me, applyBody);
            if (applyResult.getCode() == null || applyResult.getCode() != 200 || applyResult.getData() == null) {
                throw new RuntimeException(applyResult.getMessage() == null ? "发起审批失败" : applyResult.getMessage());
            }
            ot.setInstanceId(applyResult.getData());
            ot.setUpdateTime(java.time.LocalDateTime.now());
            overtimeMapper.updateById(ot);

            return Result.success("加班申请已提交审批", applyResult.getData());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 我的加班申请列表
     */
    @GetMapping("/overtime/my")
    public Result<List<Map<String, Object>>> myOvertime() {
        try {
            Long empId = getCurrentEmpId(null);
            List<Overtime> list = overtimeMapper.selectList(new LambdaQueryWrapper<Overtime>()
                    .eq(Overtime::getEmpId, empId)
                    .orderByDesc(Overtime::getOvertimeDate));
            return Result.success(fillOvertimeRows(list));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 加班申请列表（管理端）：admin 全部 / manager 本部门
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/overtime/list")
    public Result<List<Map<String, Object>>> overtimeList() {
        try {
            List<Overtime> list;
            if (UserContext.isManager() && UserContext.getDeptId() != null) {
                Set<Long> deptEmpIds = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                                .eq(Employee::getDeptId, UserContext.getDeptId()))
                        .stream().map(Employee::getId).collect(Collectors.toSet());
                list = deptEmpIds.isEmpty() ? new ArrayList<>() : overtimeMapper.selectList(
                        new LambdaQueryWrapper<Overtime>()
                                .in(Overtime::getEmpId, deptEmpIds)
                                .orderByDesc(Overtime::getOvertimeDate));
            } else {
                list = overtimeMapper.selectList(new LambdaQueryWrapper<Overtime>()
                        .orderByDesc(Overtime::getOvertimeDate));
            }
            return Result.success(fillOvertimeRows(list));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private List<Map<String, Object>> fillOvertimeRows(List<Overtime> list) {
        List<Long> empIds = list.stream().map(Overtime::getEmpId).distinct().collect(Collectors.toList());
        Map<Long, Employee> empMap = empIds.isEmpty() ? new HashMap<>()
                : employeeMapper.selectList(new LambdaQueryWrapper<Employee>().in(Employee::getId, empIds))
                        .stream().collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Overtime ot : list) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", ot.getId());
            row.put("empId", ot.getEmpId());
            Employee emp = empMap.get(ot.getEmpId());
            row.put("empName", emp != null ? emp.getName() : "-");
            row.put("overtimeDate", ot.getOvertimeDate());
            row.put("hours", ot.getHours());
            row.put("reason", ot.getReason());
            row.put("status", ot.getStatus());
            row.put("instanceId", ot.getInstanceId());
            row.put("createTime", ot.getCreateTime());
            rows.add(row);
        }
        return rows;
    }

    // ==================== P4-2 请假额度 ====================

    /**
     * 我的本年度请假额度（年假）
     */
    @GetMapping("/leave-quota/my")
    public Result<Map<String, Object>> myLeaveQuota() {
        try {
            Long empId = getCurrentEmpId(null);
            return Result.success(quotaRow(empId, LeaveQuotaService.currentYear()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 额度列表（管理端）：admin 全员 / manager 本部门
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/leave-quota/list")
    public Result<List<Map<String, Object>>> quotaList(@RequestParam(required = false) Integer year) {
        try {
            int targetYear = year == null ? LeaveQuotaService.currentYear() : year;
            List<Employee> emps;
            if (UserContext.isManager() && UserContext.getDeptId() != null) {
                emps = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getDeptId, UserContext.getDeptId())
                        .orderByAsc(Employee::getId));
            } else {
                emps = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                        .orderByAsc(Employee::getId));
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Employee emp : emps) {
                rows.add(quotaRow(emp.getId(), targetYear));
            }
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 单人员额度行：附带工号/姓名/部门，便于管理端直读
     */
    private Map<String, Object> quotaRow(Long empId, int year) {
        LeaveQuota quota = leaveQuotaService.getOrCreate(empId, year, LeaveQuotaService.TYPE_ANNUAL);
        Employee emp = employeeMapper.selectById(empId);
        String deptName = "";
        if (emp != null && emp.getDeptId() != null) {
            Department dept = departmentMapper.selectById(emp.getDeptId());
            deptName = dept != null ? dept.getDeptName() : "";
        }
        Map<String, Object> row = new HashMap<>();
        row.put("empId", empId);
        row.put("empNo", emp != null ? emp.getEmpNo() : "-");
        row.put("empName", emp != null ? emp.getName() : "-");
        row.put("deptName", deptName);
        row.put("year", year);
        row.put("quotaType", quota.getQuotaType());
        row.put("totalDays", quota.getTotalDays());
        row.put("usedDays", quota.getUsedDays());
        row.put("remainDays", quota.getTotalDays().subtract(quota.getUsedDays()));
        return row;
    }

    /**
     * P4-3 部门数据权限：manager 只能操作本部门员工的考勤数据
     */
    private void checkDeptScope(Long empId) {
        if (!UserContext.isManager()) {
            return;
        }
        if (UserContext.getDeptId() == null) {
            throw new RuntimeException("当前账号未绑定部门，无法查看考勤数据");
        }
        Employee emp = employeeMapper.selectById(empId);
        if (emp == null || !UserContext.getDeptId().equals(emp.getDeptId())) {
            throw new RuntimeException("只能查看本部门员工的考勤数据");
        }
    }

    /**
     * 从token中获取员工ID（P3-2 修正：按 sys_user.empId 绑定关系取真实员工，
     * 避免误用 user.id 造成替他人打卡/错位打卡）
     */
    private Long getCurrentEmpId(HttpServletRequest request) {
        String token = request == null ? null : request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            String me = UserContext.get();
            User user = me == null ? null : userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, me));
            if (user == null || user.getEmpId() == null) {
                throw new RuntimeException("当前账号未绑定员工档案");
            }
            return user.getEmpId();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        if (user == null || user.getEmpId() == null) {
            throw new RuntimeException("当前账号未绑定员工档案，无法打卡");
        }
        return user.getEmpId();
    }
}
