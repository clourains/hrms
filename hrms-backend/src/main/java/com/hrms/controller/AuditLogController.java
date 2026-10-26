package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.AuditLogRecord;
import com.hrms.mapper.AuditLogRecordMapper;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 审计日志查询 Controller（P1-1），仅管理员可见
 */
@RestController
@RequestMapping("/auditLog")
@RequireRole("admin")
public class AuditLogController {

    @Autowired
    private AuditLogRecordMapper auditLogRecordMapper;

    /**
     * 分页查询审计日志，支持按操作人/模块/时间范围过滤
     */
    @GetMapping("/list")
    public Result<Page<AuditLogRecord>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            LambdaQueryWrapper<AuditLogRecord> wrapper = new LambdaQueryWrapper<AuditLogRecord>()
                    .like(StringUtils.hasText(username), AuditLogRecord::getUsername, username)
                    .like(StringUtils.hasText(module), AuditLogRecord::getModule, module)
                    .ge(StringUtils.hasText(startTime), AuditLogRecord::getCreateTime,
                            parseStart(startTime))
                    .le(StringUtils.hasText(endTime), AuditLogRecord::getCreateTime,
                            parseEnd(endTime))
                    .orderByDesc(AuditLogRecord::getCreateTime);
            Page<AuditLogRecord> page = auditLogRecordMapper.selectPage(new Page<>(current, size), wrapper);
            return Result.success(page);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private LocalDateTime parseStart(String startTime) {
        return StringUtils.hasText(startTime)
                ? LocalDateTime.parse(startTime + (startTime.length() == 10 ? "T00:00:00" : ""))
                : null;
    }

    private LocalDateTime parseEnd(String endTime) {
        return StringUtils.hasText(endTime)
                ? LocalDateTime.parse(endTime + (endTime.length() == 10 ? "T23:59:59" : ""))
                : null;
    }
}
