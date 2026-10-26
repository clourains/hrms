package com.hrms.controller;

import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Department;
import com.hrms.service.DepartmentService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门Controller
 */
@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取部门树形结构
     */
    @GetMapping("/tree")
    public Result<List<Department>> tree() {
        try {
            List<Department> tree = departmentService.tree();
            return Result.success(tree);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有部门列表
     */
    @GetMapping("/list")
    public Result<List<Department>> list() {
        try {
            List<Department> list = departmentService.listAll();
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID获取部门信息
     */
    @GetMapping("/{id}")
    public Result<Department> getById(@PathVariable Long id) {
        try {
            Department department = departmentService.getById(id);
            return Result.success(department);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增部门
     */
    @RequireRole("admin")
    @AuditLog(module = "部门管理", action = "新增")
    @PostMapping
    public Result<Void> save(@RequestBody Department department) {
        try {
            boolean result = departmentService.save(department);
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
     * 更新部门信息
     */
    @RequireRole("admin")
    @AuditLog(module = "部门管理", action = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody Department department) {
        try {
            boolean result = departmentService.update(department);
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
     * 根据ID删除部门
     */
    @RequireRole("admin")
    @AuditLog(module = "部门管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            boolean result = departmentService.deleteById(id);
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
