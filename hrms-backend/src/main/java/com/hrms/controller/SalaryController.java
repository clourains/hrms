package com.hrms.controller;

import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Salary;
import com.hrms.entity.User;
import com.hrms.mapper.UserMapper;
import com.hrms.service.SalaryService;
import com.hrms.service.impl.PayslipService;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
     * 根据ID获取薪资记录
     */
    @GetMapping("/{id}")
    public Result<Salary> getById(@PathVariable Long id) {
        try {
            Salary salary = salaryService.getById(id);
            return Result.success(salary);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取员工薪资列表
     */
    @GetMapping("/listByEmpId")
    public Result<List<Salary>> listByEmpId(@RequestParam Long empId) {
        try {
            List<Salary> list = salaryService.listByEmpId(empId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据月份查询薪资
     */
    @GetMapping("/listByMonth")
    public Result<List<Salary>> listByMonth(@RequestParam String salaryMonth) {
        try {
            List<Salary> list = salaryService.listByMonth(salaryMonth);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 月度薪资统计
     */
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
