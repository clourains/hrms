package com.hrms.controller;

import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.dto.EmployeeExitQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.EmployeeExit;
import com.hrms.service.EmployeeExitService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 入离管理：离职 / 辞退单据
 *
 * 两个页面共用同一套接口，用 exitKind 区分（1 离职、2 辞退）。
 */
@RestController
@RequestMapping("/employee-exit")
@RequireRole("admin")
public class EmployeeExitController {

    @Autowired
    private EmployeeExitService employeeExitService;

    @PostMapping("/page")
    public Result<PageResult<EmployeeExit>> page(@RequestBody EmployeeExitQueryDTO query) {
        try {
            return Result.success(employeeExitService.page(query));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam(required = false) Integer kind) {
        try {
            return Result.success(employeeExitService.statistics(kind));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    @AuditLog(module = "入离管理", action = "发起单据")
    public Result<EmployeeExit> create(@RequestBody EmployeeExit exit) {
        try {
            return Result.success("单据已提交，等待审批", employeeExitService.create(exit));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    @AuditLog(module = "入离管理", action = "修改单据")
    public Result<Void> update(@RequestBody EmployeeExit exit) {
        try {
            employeeExitService.update(exit);
            return Result.success("单据已更新", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    @AuditLog(module = "入离管理", action = "审批通过")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            employeeExitService.approve(id, body == null ? null : body.get("remark"));
            return Result.success("已批准，待办理生效", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    @AuditLog(module = "入离管理", action = "审批驳回")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            employeeExitService.reject(id, body == null ? null : body.get("remark"));
            return Result.success("已驳回", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/effect")
    @AuditLog(module = "入离管理", action = "办理生效")
    public Result<Void> effect(@PathVariable Long id) {
        try {
            employeeExitService.effect(id);
            return Result.success("已生效：员工状态置为离职并停用登录账号", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    @AuditLog(module = "入离管理", action = "撤销单据")
    public Result<Void> cancel(@PathVariable Long id) {
        try {
            employeeExitService.cancel(id);
            return Result.success("单据已撤销", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "入离管理", action = "删除单据")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            employeeExitService.delete(id);
            return Result.success("单据已删除", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
