package com.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Employee;
import com.hrms.entity.User;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * SMTP 邮件服务（P6-3）
 *
 * 全部参数从 sys_config 动态读取（mail.enabled/host/port/username/password），
 * 不依赖 application.yml，管理员在「参数配置」页改完即生效。
 * 未启用（mail.enabled != true）时所有发送调用直接跳过返回 false，不抛异常。
 */
@Service
public class MailService {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 邮件是否已启用且配置完整
     */
    public boolean isEnabled() {
        if (!"true".equalsIgnoreCase(sysConfigService.getValue("mail.enabled", "false"))) {
            return false;
        }
        return StringUtils.hasText(sysConfigService.getValue("mail.host", ""))
                && StringUtils.hasText(sysConfigService.getValue("mail.username", ""));
    }

    /**
     * 发送纯文本邮件。未启用或参数不全时静默跳过。
     *
     * @return true 已发出 / false 跳过或失败
     */
    public boolean send(String to, String subject, String text) {
        if (!isEnabled() || !StringUtils.hasText(to)) {
            return false;
        }
        try {
            String host = sysConfigService.getValue("mail.host", "");
            String port = sysConfigService.getValue("mail.port", "465");
            String username = sysConfigService.getValue("mail.username", "");
            String password = sysConfigService.getValue("mail.password", "");

            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(Integer.parseInt(port.trim()));
            sender.setUsername(username);
            sender.setPassword(password);
            sender.setDefaultEncoding("UTF-8");

            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            // 465 走 SSL，587 走 STARTTLS；按端口自动选择
            if ("465".equals(port.trim())) {
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(username);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            sender.send(message);
            return true;
        } catch (Exception e) {
            // 邮件失败不阻断主流程（如工资条推送），仅记录
            org.slf4j.LoggerFactory.getLogger(MailService.class)
                    .warn("发送邮件失败 to={}: {}", to, e.getMessage());
            return false;
        }
    }

    /**
     * 按登录用户名解析员工邮箱：sys_user.emp_id → employee.email
     *
     * @return 邮箱地址，未绑定或未填邮箱返回 null
     */
    public String resolveEmailByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || user.getEmpId() == null) {
            return null;
        }
        Employee emp = employeeMapper.selectById(user.getEmpId());
        return (emp != null && StringUtils.hasText(emp.getEmail())) ? emp.getEmail() : null;
    }

    /**
     * 邮件连通性测试（admin 在参数配置页点「测试发信」）
     */
    public Result<String> testSend(String to) {
        if (!isEnabled()) {
            return Result.error("邮件未启用或配置不完整，请先配置 mail.enabled/host/username/password");
        }
        boolean ok = send(to, "HRMS 邮件测试", "这是一封来自 HRMS 系统的测试邮件，说明 SMTP 配置已生效。");
        return ok ? Result.success("测试邮件已发送，请查收", null)
                : Result.error("发送失败，请检查 SMTP 参数和授权码");
    }
}
