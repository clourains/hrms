package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.annotation.AuditLog;
import com.hrms.entity.User;
import com.hrms.mapper.UserMapper;
import com.hrms.service.ApprovalService;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 多级审批流 Controller（P3-1）
 *
 * 权限模型：所有登录用户可发起/审批（节点校验在 Service 内完成），不区分角色。
 */
@RestController
@RequestMapping("/approval")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 发起审批：{bizType, title, detail(JSON对象或文本), approvers:[用户名...]}
     */
    @AuditLog(module = "审批中心", action = "发起申请")
    @PostMapping("/apply")
    public Result<Long> apply(@RequestBody Map<String, Object> body) {
        return approvalService.apply(UserContext.get(), body);
    }

    /**
     * 通过当前节点
     */
    @AuditLog(module = "审批中心", action = "审批通过")
    @PostMapping("/approve/{id}")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody(required = false) Map<String, String> body) {
        String comment = body == null ? null : body.get("comment");
        return approvalService.approve(UserContext.get(), id, comment);
    }

    /**
     * 驳回
     */
    @AuditLog(module = "审批中心", action = "审批驳回")
    @PostMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestBody(required = false) Map<String, String> body) {
        String comment = body == null ? null : body.get("comment");
        return approvalService.reject(UserContext.get(), id, comment);
    }

    /**
     * 申请人撤销
     */
    @AuditLog(module = "审批中心", action = "撤销申请")
    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        return approvalService.cancel(UserContext.get(), id);
    }

    /**
     * 我发起的
     */
    @GetMapping("/my-apply")
    public Result<List<Map<String, Object>>> myApply() {
        return approvalService.myApply(UserContext.get());
    }

    /**
     * 待我审批
     */
    @GetMapping("/todo")
    public Result<List<Map<String, Object>>> todo() {
        return approvalService.todo(UserContext.get());
    }

    /**
     * 我已处理
     */
    @GetMapping("/done")
    public Result<List<Map<String, Object>>> done() {
        return approvalService.done(UserContext.get());
    }

    /**
     * 审批详情
     */
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return approvalService.detail(UserContext.get(), id);
    }

    /**
     * 审批人候选列表（发起申请时选择，所有登录用户可用，排除自己）
     */
    @GetMapping("/approvers")
    public Result<List<Map<String, Object>>> approvers() {
        try {
            String me = UserContext.get();
            List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>().orderByAsc(User::getUsername));
            List<Map<String, Object>> rows = users.stream()
                    .filter(u -> !u.getUsername().equals(me))
                    .map(u -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("username", u.getUsername());
                        row.put("nickname", u.getNickname());
                        row.put("role", u.getRole());
                        return row;
                    }).collect(Collectors.toList());
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
