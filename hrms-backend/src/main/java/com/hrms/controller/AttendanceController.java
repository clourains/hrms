package com.hrms.controller;

import com.hrms.entity.Attendance;
import com.hrms.service.AttendanceService;
import com.hrms.util.JwtUtil;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

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
     * 获取员工月度考勤列表
     */
    @GetMapping("/list")
    public Result<List<Attendance>> list(@RequestParam Long empId, @RequestParam String month) {
        try {
            List<Attendance> list = attendanceService.listByEmpIdAndMonth(empId, month);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 月度考勤统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam Long empId, @RequestParam String month) {
        try {
            Map<String, Object> statistics = attendanceService.monthlyStatistics(empId, month);
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增考勤记录
     */
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

    /**
     * 从token中获取员工ID
     */
    private Long getCurrentEmpId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        // 这里简化处理，实际应该通过userId查询员工ID
        return userId;
    }
}
