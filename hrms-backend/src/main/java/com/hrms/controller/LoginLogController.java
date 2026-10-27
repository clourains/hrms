package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.LoginLog;
import com.hrms.mapper.LoginLogMapper;
import com.hrms.util.ExcelExportUtil;
import com.hrms.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录日志查询 Controller（P5-2），仅管理员可见
 */
@RestController
@RequestMapping("/loginLog")
@RequireRole("admin")
public class LoginLogController {

    @Autowired
    private LoginLogMapper loginLogMapper;

    /**
     * 分页查询登录日志，支持按用户名/结果/时间范围过滤
     */
    @GetMapping("/list")
    public Result<Page<LoginLog>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                    .like(StringUtils.hasText(username), LoginLog::getUsername, username)
                    .eq(success != null, LoginLog::getSuccess, success)
                    .ge(StringUtils.hasText(startTime), LoginLog::getCreateTime, parseTime(startTime, true))
                    .le(StringUtils.hasText(endTime), LoginLog::getCreateTime, parseTime(endTime, false))
                    .orderByDesc(LoginLog::getCreateTime);
            Page<LoginLog> page = loginLogMapper.selectPage(new Page<>(current, size), wrapper);
            return Result.success(page);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private LocalDateTime parseTime(String time, boolean isStart) {
        if (!StringUtils.hasText(time)) {
            return null;
        }
        return LocalDateTime.parse(time + (time.length() == 10 ? (isStart ? "T00:00:00" : "T23:59:59") : ""));
    }

    /**
     * 按当前筛选条件导出 Excel（P8-A）
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String username,
                       @RequestParam(required = false) Integer success,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                .like(StringUtils.hasText(username), LoginLog::getUsername, username)
                .eq(success != null, LoginLog::getSuccess, success)
                .ge(StringUtils.hasText(startTime), LoginLog::getCreateTime, parseTime(startTime, true))
                .le(StringUtils.hasText(endTime), LoginLog::getCreateTime, parseTime(endTime, false))
                .orderByDesc(LoginLog::getCreateTime);
        List<LoginLog> list = loginLogMapper.selectList(wrapper);
        List<List<Object>> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (LoginLog l : list) {
            rows.add(List.of(
                    l.getCreateTime() == null ? "" : l.getCreateTime().format(fmt),
                    l.getUsername() == null ? "" : l.getUsername(),
                    l.getSuccess() != null && l.getSuccess() == 1 ? "成功" : "失败",
                    l.getMsg() == null ? "" : l.getMsg(),
                    l.getIp() == null ? "" : l.getIp(),
                    l.getUserAgent() == null ? "" : l.getUserAgent()
            ));
        }
        ExcelExportUtil.write(response, "登录日志.xlsx",
                List.of("登录时间", "用户名", "结果", "说明", "IP", "浏览器"),
                rows);
    }
}
