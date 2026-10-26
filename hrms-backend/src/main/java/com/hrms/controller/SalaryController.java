package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Employee;
import com.hrms.entity.Message;
import com.hrms.entity.Salary;
import com.hrms.entity.User;
import com.hrms.mapper.MessageMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.service.SalaryService;
import com.hrms.service.impl.PayslipService;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 薪资Controller
 */
@RestController
@RequestMapping("/salary")
public class SalaryController {

    @Autowired
    private SalaryService salaryService;

    @Autowired
    private PayslipService payslipService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.hrms.mapper.EmployeeMapper employeeMapper;

    @Autowired
    private MessageMapper messageMapper;

    /**
     * 下载工资条 PDF（P2-3）：管理员可下载任意员工，普通用户只能下载自己
     */
    @GetMapping("/payslip")
    public void payslip(@RequestParam Long empId,
                        @RequestParam String salaryMonth,
                        HttpServletResponse response) throws Exception {
        try {
            Long targetEmpId = empId;
            if (!UserContext.isAdmin()) {
                String me = UserContext.get();
                User user = me == null ? null : userMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                                .eq(User::getUsername, me));
                if (user == null || user.getEmpId() == null) {
                    writeJsonError(response, 403, "当前账号未绑定员工，无权查看工资条");
                    return;
                }
                targetEmpId = user.getEmpId();
            }
            String empName = "员工" + targetEmpId;
            com.hrms.entity.Employee emp = employeeMapper.selectById(targetEmpId);
            if (emp != null && emp.getName() != null) {
                empName = emp.getName();
            }
            byte[] pdf = payslipService.buildPayslip(targetEmpId, salaryMonth);
            String filename = URLEncoder.encode("工资条_" + empName + "_" + salaryMonth + ".pdf",
                    StandardCharsets.UTF_8);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + filename);
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
        } catch (Exception e) {
            writeJsonError(response, 500, e.getMessage());
        }
    }

    private void writeJsonError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" 
                + (message == null ? "" : message.replace("\"", "'")) + "\",\"data\":null}");
    }

    /**
     * 根据ID获取薪资记录（P3-2 越权收紧：非 admin 仅可查本人记录）
     */
    @GetMapping("/{id}")
    public Result<Salary> getById(@PathVariable Long id) {
        try {
            Salary salary = salaryService.getById(id);
            if (salary != null && !UserContext.isAdmin()
                    && !salary.getEmpId().equals(currentEmpIdOrThrow())) {
                return Result.error("无权查看他人薪资记录");
            }
            return Result.success(salary);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取员工薪资列表（P3-2 越权收紧：非 admin 强制查本人）
     */
    @GetMapping("/listByEmpId")
    public Result<List<Salary>> listByEmpId(@RequestParam Long empId) {
        try {
            Long target = empId;
            if (!UserContext.isAdmin()) {
                target = currentEmpIdOrThrow();
            }
            List<Salary> list = salaryService.listByEmpId(target);
            return Result.success(list);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据月份查询薪资（P3-2 越权收紧：非 admin 仅返回本人记录）
     */
    @GetMapping("/listByMonth")
    public Result<List<Salary>> listByMonth(@RequestParam String salaryMonth) {
        try {
            List<Salary> list = salaryService.listByMonth(salaryMonth);
            if (!UserContext.isAdmin()) {
                Long myEmpId = currentEmpIdOrThrow();
                list = list.stream().filter(s -> s.getEmpId().equals(myEmpId)).collect(java.util.stream.Collectors.toList());
            }
            return Result.success(list);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 月度薪资统计（全员汇总，仅管理员）
     */
    @RequireRole("admin")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam String salaryMonth) {
        try {
            Map<String, Object> statistics = salaryService.monthlyStatistics(salaryMonth);
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 当前登录人绑定的员工ID（未绑定抛业务异常）
     */
    private Long currentEmpIdOrThrow() {
        String me = UserContext.get();
        User user = me == null ? null : userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, me));
        if (user == null || user.getEmpId() == null) {
            throw new RuntimeException("当前账号未绑定员工档案，无权查看薪资");
        }
        return user.getEmpId();
    }

    /**
     * 一键计算月度薪资（P2-2）：考勤扣款 + 绩效奖金联动，已有记录的员工跳过
     */
    @RequireRole("admin")
    @AuditLog(module = "薪资管理", action = "一键计算")
    @PostMapping("/calculate")
    public Result<Map<String, Object>> calculate(@RequestBody Map<String, String> body) {
        try {
            String month = body.get("salaryMonth");
            if (month == null || !month.matches("\\d{4}-\\d{2}")) {
                return Result.error("请提供正确的月份（yyyy-MM）");
            }
            return Result.success("计算完成", salaryService.calculateMonth(month));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 一键计算并发放工资条（P4-4）：计算完成后为每个绑定账号的员工发站内信，
     * 内容含应发/实发金额，员工到「薪资管理」自助下载 PDF 工资条；
     * 同月同标题已推送过的员工自动跳过，避免重复点击刷屏。
     */
    @RequireRole("admin")
    @AuditLog(module = "薪资管理", action = "计算并推送工资条")
    @PostMapping("/calculate-and-notify")
    public Result<Map<String, Object>> calculateAndNotify(@RequestBody Map<String, String> body) {
        try {
            String month = body.get("salaryMonth");
            if (month == null || !month.matches("\\d{4}-\\d{2}")) {
                return Result.error("请提供正确的月份（yyyy-MM）");
            }
            Map<String, Object> calc = salaryService.calculateMonth(month);

            List<Salary> list = salaryService.listByMonth(month);
            List<Long> empIds = list.stream().map(Salary::getEmpId).distinct().collect(Collectors.toList());
            Map<Long, String> usernameMap = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .in(User::getEmpId, empIds.isEmpty() ? List.of(0L) : empIds))
                    .stream().filter(u -> u.getEmpId() != null)
                    .collect(Collectors.toMap(User::getEmpId, User::getUsername, (a, b) -> a));

            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime now = LocalDateTime.now();
            int notified = 0;
            for (Salary s : list) {
                String username = usernameMap.get(s.getEmpId());
                if (username == null || username.isEmpty()) {
                    continue; // 未绑定登录账号的员工跳过
                }
                String title = month + " 工资条已发放";
                Long dup = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiver, username)
                        .eq(Message::getTitle, title)
                        .ge(Message::getCreateTime, todayStart));
                if (dup != null && dup > 0) {
                    continue;
                }
                BigDecimal gross = nvl(s.getBaseSalary()).add(nvl(s.getPerformanceBonus()))
                        .add(nvl(s.getOvertimePay())).add(nvl(s.getAllowance()));
                String content = String.format(
                        "您的 %s 工资条已生成：应发 ¥%s，实发 ¥%s。请前往「薪资管理」页面查看明细并下载 PDF 工资条。",
                        month, gross.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        nvl(s.getNetSalary()).setScale(2, RoundingMode.HALF_UP).toPlainString());
                Message msg = new Message();
                msg.setReceiver(username);
                msg.setTitle(title);
                msg.setContent(content);
                msg.setMsgType("薪资");
                msg.setSender("system");
                msg.setIsRead(0);
                msg.setCreateTime(now);
                msg.setUpdateTime(now);
                messageMapper.insert(msg);
                notified++;
            }
            calc.put("notified", notified);
            return Result.success("计算并推送完成", calc);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 新增薪资记录
     */
    @RequireRole("admin")
    @AuditLog(module = "薪资管理", action = "新增")
    @PostMapping
    public Result<Void> save(@RequestBody Salary salary) {
        try {
            boolean result = salaryService.save(salary);
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
     * 更新薪资记录
     */
    @RequireRole("admin")
    @AuditLog(module = "薪资管理", action = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody Salary salary) {
        try {
            boolean result = salaryService.update(salary);
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
     * 根据ID删除薪资记录
     */
    @RequireRole("admin")
    @AuditLog(module = "薪资管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            boolean result = salaryService.deleteById(id);
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
