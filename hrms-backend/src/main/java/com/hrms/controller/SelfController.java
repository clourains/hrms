package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Attendance;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.entity.Salary;
import com.hrms.entity.User;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.service.AttendanceService;
import com.hrms.service.LeaveQuotaService;
import com.hrms.service.SalaryService;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工自助端 Controller（P3-2）
 *
 * 全部接口以当前登录人为准：非 admin 只能看到自己绑定的员工数据。
 */
@RestController
@RequestMapping("/self")
public class SelfController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private SalaryService salaryService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private LeaveQuotaService leaveQuotaService;

    /**
     * 个人资料：账号信息 + 员工档案 + 部门
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        try {
            String me = UserContext.get();
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, me));
            if (user == null) {
                return Result.error("用户不存在");
            }
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", user.getUsername());
            userMap.put("nickname", user.getNickname());
            userMap.put("role", user.getRole());
            userMap.put("bindEmpId", user.getEmpId());
            data.put("user", userMap);

            if (user.getEmpId() == null) {
                data.put("employee", null);
                data.put("department", null);
                return Result.success(data);
            }
            Employee emp = employeeMapper.selectById(user.getEmpId());
            data.put("employee", emp);
            if (emp != null && emp.getDeptId() != null) {
                Department dept = departmentMapper.selectById(emp.getDeptId());
                data.put("department", dept);
            } else {
                data.put("department", null);
            }
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 我的薪资记录（全部月份，仅本人）
     */
    @GetMapping("/salary")
    public Result<List<Salary>> mySalary() {
        try {
            Long empId = requireMyEmpId();
            return Result.success(salaryService.listByEmpId(empId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 我的某月考勤：统计 + 打卡明细
     */
    @GetMapping("/attendance")
    public Result<Map<String, Object>> myAttendance(@RequestParam String month) {
        try {
            Long empId = requireMyEmpId();
            Map<String, Object> data = new HashMap<>();
            data.put("statistics", attendanceService.monthlyStatistics(empId, month));
            data.put("records", attendanceService.listByEmpIdAndMonth(empId, month));
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 我的本年度假期额度（P4-2，年假）
     */
    @GetMapping("/leave-quota")
    public Result<Map<String, Object>> myLeaveQuota() {
        try {
            Long empId = requireMyEmpId();
            int year = java.time.LocalDate.now().getYear();
            com.hrms.entity.LeaveQuota quota = leaveQuotaService.getOrCreate(
                    empId, year, LeaveQuotaService.TYPE_ANNUAL);
            Map<String, Object> data = new HashMap<>();
            data.put("year", year);
            data.put("quotaType", quota.getQuotaType());
            data.put("totalDays", quota.getTotalDays());
            data.put("usedDays", quota.getUsedDays());
            data.put("remainDays", quota.getTotalDays().subtract(quota.getUsedDays()));
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 今日打卡状态（已打上班/下班卡与否）
     */
    @GetMapping("/attendance-today")
    public Result<Map<String, Object>> today() {
        try {
            Long empId = requireMyEmpId();
            List<Attendance> today = attendanceService.listByEmpIdAndMonth(empId,
                    LocalDate.now().toString().substring(0, 7));
            Attendance todayRec = today == null ? null : today.stream()
                    .filter(a -> a.getAttendDate() != null && LocalDate.now().equals(a.getAttendDate()))
                    .findFirst().orElse(null);
            Map<String, Object> data = new HashMap<>();
            data.put("checkedIn", todayRec != null && todayRec.getCheckInTime() != null);
            data.put("checkedOut", todayRec != null && todayRec.getCheckOutTime() != null);
            data.put("today", todayRec);
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 当前登录人绑定的员工ID；未绑定则抛业务异常
     */
    private Long requireMyEmpId() {
        String me = UserContext.get();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, me));
        if (user == null || user.getEmpId() == null) {
            throw new RuntimeException("当前账号未绑定员工档案");
        }
        return user.getEmpId();
    }
}
