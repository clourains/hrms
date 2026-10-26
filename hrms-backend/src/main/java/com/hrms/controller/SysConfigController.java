package com.hrms.controller;

import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.SysConfig;
import com.hrms.service.SysConfigService;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统参数配置 Controller（P5-3），仅管理员可见
 */
@RestController
@RequestMapping("/config")
@RequireRole("admin")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 查询全部参数
     */
    @GetMapping("/list")
    public Result<List<SysConfig>> list() {
        try {
            return Result.success(sysConfigService.listAll());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新参数值
     */
    @PutMapping("/update")
    @AuditLog(module = "系统配置", action = "修改")
    public Result<Void> update(@RequestBody Map<String, String> body) {
        try {
            String key = body.get("configKey");
            String value = body.get("configValue");
            if (key == null || key.trim().isEmpty() || value == null) {
                return Result.error("参数键和参数值不能为空");
            }
            sysConfigService.updateValue(key.trim(), value.trim(), UserContext.getOrDefault("unknown"));
            return Result.success("参数已更新", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
