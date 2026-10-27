package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.AuditLogRecord;
import com.hrms.mapper.AuditLogRecordMapper;
import com.hrms.util.ExcelExportUtil;
import com.hrms.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 审计日志查询 Controller（P1-1 / P8-A），仅管理员可见
 * P8-A：加 success 筛选 + 通用 export 导出 Excel
 */
@RestController
@RequestMapping("/auditLog")
@RequireRole("admin")
public class AuditLogController {

    @Autowired
    private AuditLogRecordMapper auditLogRecordMapper;

    /**
     * 分页查询审计日志
     */
    @GetMapping("/list")
    public Result<Page<AuditLogRecord>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            Page<AuditLogRecord> page = auditLogRecordMapper.selectPage(
                    new Page<>(current, size),
                    buildWrapper(username, module, success, startTime, endTime));
            return Result.success(page);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 按当前筛选条件导出 Excel（P8-A）
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String username,
                       @RequestParam(required = false) String module,
                       @RequestParam(required = false) Integer success,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) throws Exception {
        List<AuditLogRecord> list = auditLogRecordMapper.selectList(
                buildWrapper(username, module, success, startTime, endTime));
        List<List<Object>> rows = new java.util.ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AuditLogRecord r : list) {
            rows.add(List.of(
                    r.getCreateTime() == null ? "" : r.getCreateTime().format(fmt),
                    r.getUsername() == null ? "" : r.getUsername(),
                    r.getModule() == null ? "" : r.getModule(),
                    r.getAction() == null ? "" : r.getAction(),
                    r.getDetail() == null ? "" : r.getDetail(),
                    r.getSuccess() != null && r.getSuccess() == 1 ? "成功" : "失败",
                    r.getErrorMsg() == null ? "" : r.getErrorMsg(),
                    r.getCostMs() == null ? "" : r.getCostMs() + "ms",
                    r.getIp() == null ? "" : r.getIp()
            ));
        }
        ExcelExportUtil.write(response, "审计日志.xlsx",
                List.of("操作时间", "操作人", "模块", "操作", "操作详情", "结果", "失败原因", "耗时", "IP"),
                rows);
    }

    private LambdaQueryWrapper<AuditLogRecord> buildWrapper(
            String username, String module, Integer success, String startTime, String endTime) {
        return new LambdaQueryWrapper<AuditLogRecord>()
                .like(StringUtils.hasText(username), AuditLogRecord::getUsername, username)
                .eq(success != null, AuditLogRecord::getSuccess, success)
                .like(StringUtils.hasText(module), AuditLogRecord::getModule, module)
                .ge(StringUtils.hasText(startTime), AuditLogRecord::getCreateTime,
                        parseStart(startTime))
                .le(StringUtils.hasText(endTime), AuditLogRecord::getCreateTime,
                        parseEnd(endTime))
                .orderByDesc(AuditLogRecord::getCreateTime);
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
