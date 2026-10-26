package com.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.User;
import com.hrms.service.UserService;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理 Controller（P5-1），仅管理员可见
 */
@RestController
@RequestMapping("/user")
@RequireRole("admin")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 分页查询用户列表，支持按用户名/昵称关键字、角色过滤
     */
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        try {
            return Result.success(userService.pageQuery(current, size, keyword, role));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新建用户
     */
    @PostMapping
    @AuditLog(module = "用户管理", action = "新增")
    public Result<User> create(@RequestBody User user) {
        try {
            return Result.success("用户创建成功", userService.createUser(user));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户（昵称/角色/绑定员工/状态）
     */
    @PutMapping
    @AuditLog(module = "用户管理", action = "修改")
    public Result<Void> update(@RequestBody User user) {
        try {
            userService.updateUser(user, UserContext.getOrDefault("unknown"));
            return Result.success("用户已更新", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 逻辑删除用户
     */
    @DeleteMapping("/{id}")
    @AuditLog(module = "用户管理", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            userService.deleteUser(id, UserContext.getOrDefault("unknown"));
            return Result.success("用户已删除", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重置密码
     */
    @PostMapping("/resetPassword/{id}")
    @AuditLog(module = "用户管理", action = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String newPassword = body != null ? body.get("password") : null;
            userService.resetPassword(id, newPassword);
            return Result.success("密码已重置", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
