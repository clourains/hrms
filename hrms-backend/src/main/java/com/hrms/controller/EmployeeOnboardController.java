package com.hrms.controller;

import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.dto.EmployeeOnboardQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.EmployeeOnboard;
import com.hrms.service.EmployeeOnboardService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 入职登记（入离管理之「入」）
 *
 * 登记台账 + 员工档案关联：关联已有档案 / 一键新建档案（含试用期）。
 */
@RestController
@RequestMapping("/employee-onboard")
@RequireRole("admin")
public class EmployeeOnboardController {

    @Autowired
    private EmployeeOnboardService employeeOnboardService;

    @PostMapping("/page")
    public Result<PageResult<EmployeeOnboard>> page(@RequestBody EmployeeOnboardQueryDTO query) {
        try {
            return Result.success(employeeOnboardService.page(query));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        try {
            return Result.success(employeeOnboardService.statistics());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    @AuditLog(module = "入离管理", action = "入职登记")
    public Result<EmployeeOnboard> create(@RequestBody EmployeeOnboard onboard) {
        try {
            return Result.success("登记成功，可继续关联员工档案", employeeOnboardService.create(onboard));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    @AuditLog(module = "入离管理", action = "修改入职登记")
    public Result<Void> update(@RequestBody EmployeeOnboard onboard) {
        try {
            employeeOnboardService.update(onboard);
            return Result.success("登记信息已更新", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/link/{employeeId}")
    @AuditLog(module = "入离管理", action = "关联员工档案")
    public Result<Void> link(@PathVariable Long id, @PathVariable Long employeeId) {
        try {
            employeeOnboardService.link(id, employeeId);
            return Result.success("已关联员工档案", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/create-profile")
    @AuditLog(module = "入离管理", action = "新建员工档案")
    public Result<Map<String, Object>> createProfile(@PathVariable Long id) {
        try {
            Long employeeId = employeeOnboardService.createProfile(id);
            return Result.success("员工档案与试用期已生成", Map.of("employeeId", employeeId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "入离管理", action = "删除入职登记")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            employeeOnboardService.delete(id);
            return Result.success("登记已删除", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
