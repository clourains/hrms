package com.hrms.controller;

import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Performance;
import com.hrms.service.PerformanceService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 绩效Controller
 */
@RestController
@RequestMapping("/performance")
public class PerformanceController {

    @Autowired
    private PerformanceService performanceService;

    /**
     * 根据ID获取绩效记录
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/{id}")
    public Result<Performance> getById(@PathVariable Long id) {
        try {
            Performance performance = performanceService.getById(id);
            return Result.success(performance);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取员工绩效列表
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/listByEmpId")
    public Result<List<Performance>> listByEmpId(@RequestParam Long empId) {
        try {
            List<Performance> list = performanceService.listByEmpId(empId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据周期查询绩效
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/listByPeriod")
    public Result<List<Performance>> listByPeriod(@RequestParam String period) {
        try {
            List<Performance> list = performanceService.listByPeriod(period);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 绩效评估
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "考评")
    @PostMapping("/evaluate")
    public Result<Void> evaluate(@RequestBody Performance performance) {
        try {
            boolean result = performanceService.evaluate(performance);
            if (result) {
                return Result.success("评估成功", null);
            } else {
                return Result.error("评估失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 绩效统计
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam String period) {
        try {
            Map<String, Object> statistics = performanceService.statistics(period);
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增绩效记录
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "新增")
    @PostMapping
    public Result<Void> save(@RequestBody Performance performance) {
        try {
            boolean result = performanceService.save(performance);
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
     * 更新绩效记录
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody Performance performance) {
        try {
            boolean result = performanceService.update(performance);
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
     * 根据ID删除绩效记录
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            boolean result = performanceService.deleteById(id);
            if (result) {
                return Result.success("删除成功", null);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
