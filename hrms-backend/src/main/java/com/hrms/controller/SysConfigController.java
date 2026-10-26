package com.hrms.controller;

import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.SysConfig;
import com.hrms.service.MailService;
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

    @Autowired
    private MailService mailService;

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

    /**
     * 邮件连通性测试（P6-3）：向指定邮箱发测试邮件
     */
    @PostMapping("/testMail")
    public Result<String> testMail(@RequestBody Map<String, String> body) {
        try {
            String to = body.get("to");
            if (to == null || to.trim().isEmpty()) {
                return Result.error("请提供收件邮箱");
            }
            return mailService.testSend(to.trim());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
