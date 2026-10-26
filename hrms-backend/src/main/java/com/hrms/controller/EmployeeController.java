package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.dto.EmployeeQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.service.EmployeeService;
import com.hrms.util.Result;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 员工Controller
 */
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentMapper departmentMapper;

    /**
     * 分页查询员工列表
     */
    @PostMapping("/page")
    public Result<PageResult<Employee>> page(@RequestBody EmployeeQueryDTO queryDTO) {
        try {
            PageResult<Employee> pageResult = employeeService.pageQuery(queryDTO);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID获取员工信息
     */
    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id) {
        try {
            Employee employee = employeeService.getById(id);
            return Result.success(employee);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有员工列表
     */
    @GetMapping("/list")
    public Result<List<Employee>> list() {
        try {
            List<Employee> list = employeeService.listAll();
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增员工
     */
    @RequireRole("admin")
    @AuditLog(module = "员工管理", action = "新增")
    @PostMapping
    public Result<Void> save(@RequestBody Employee employee) {
        try {
            boolean result = employeeService.save(employee);
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
     * 更新员工信息
     */
    @RequireRole("admin")
    @AuditLog(module = "员工管理", action = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody Employee employee) {
        try {
            boolean result = employeeService.update(employee);
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
     * 根据ID删除员工
     */
    @RequireRole("admin")
    @AuditLog(module = "员工管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            boolean result = employeeService.deleteById(id);
            if (result) {
                return Result.success("删除成功", null);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量删除员工
     */
    @RequireRole("admin")
    @AuditLog(module = "员工管理", action = "批量删除")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        try {
            boolean result = employeeService.deleteBatch(ids);
            if (result) {
                return Result.success("删除成功", null);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 统计员工总数
     */
    @GetMapping("/count")
    public Result<Long> count() {
        try {
            Long count = employeeService.countTotal();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下载导入模板（P7-2）：表头 + 一行示例
     */
    @GetMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws Exception {
        ExcelWriter writer = ExcelUtil.getWriter();
        writer.writeHeadRow(List.of("工号*", "姓名*", "性别*(男/女)", "部门*(部门名称)", "职位*", "手机号*", "邮箱", "入职日期(yyyy-MM-dd)", "基本工资"));
        writer.writeRow(List.of("E1001", "张三", "男", "人事部", "专员", "13800000000", "zhangsan@example.com", "2026-01-15", "8000"));
        writer.autoSizeColumnAll();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("员工导入模板.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        writer.flush(response.getOutputStream(), true);
        writer.close();
    }

    /**
     * Excel 批量导入员工（P7-2）
     *
     * 逐行校验：必填缺失/格式错误/工号重复（库内+文件内）/部门不存在 → 记入失败行，
     * 其余行正常入库，不整批回滚。返回 { success, failed, errors:[{row, reason}] }。
     */
    @RequireRole("admin")
    @AuditLog(module = "员工管理", action = "Excel导入")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("请选择要上传的 Excel 文件");
            }
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
                return Result.error("仅支持 .xlsx / .xls 文件");
            }

            // 部门名称 → ID 映射
            Map<String, Long> deptMap = departmentMapper.selectList(
                            new LambdaQueryWrapper<Department>().select(Department::getId, Department::getDeptName))
                    .stream().collect(Collectors.toMap(Department::getDeptName, Department::getId, (a, b) -> a));

            ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
            List<List<Object>> rows = reader.read();
            reader.close();
            if (rows.size() <= 1) {
                return Result.error("Excel 中没有数据行（第一行为表头）");
            }

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            List<Map<String, Object>> errors = new ArrayList<>();
            List<Employee> toInsert = new ArrayList<>();
            Map<String, Integer> seenEmpNos = new HashMap<>();
            // 库内已有工号集合（循环外取一次，避免逐行查库）
            Set<String> existingEmpNos = employeeService.listAll().stream()
                    .map(Employee::getEmpNo).collect(Collectors.toSet());

            for (int i = 1; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                int rowNo = i + 1; // Excel 行号（含表头）
                String empNo = cell(row, 0);
                String name = cell(row, 1);
                String genderText = cell(row, 2);
                String deptName = cell(row, 3);
                String position = cell(row, 4);
                String phone = cell(row, 5);
                String email = cell(row, 6);
                String entryDateText = cell(row, 7);
                String salaryText = cell(row, 8);

                // 整行空白跳过
                if (empNo.isEmpty() && name.isEmpty() && phone.isEmpty()) {
                    continue;
                }

                String err = null;
                Integer gender = null;
                Long deptId = null;
                LocalDate entryDate = null;
                BigDecimal salary = null;

                if (empNo.isEmpty()) {
                    err = "工号不能为空";
                } else if (seenEmpNos.containsKey(empNo)) {
                    err = "工号在文件内重复（第 " + seenEmpNos.get(empNo) + " 行已出现）";
                } else if (existingEmpNos.contains(empNo)) {
                    err = "工号已存在：" + empNo;
                } else if (name.isEmpty()) {
                    err = "姓名不能为空";
                } else if (!("男".equals(genderText) || "女".equals(genderText))) {
                    err = "性别只能填 男 或 女";
                } else if ((deptId = deptMap.get(deptName)) == null) {
                    err = "部门不存在：" + deptName;
                } else if (position.isEmpty()) {
                    err = "职位不能为空";
                } else if (phone.isEmpty()) {
                    err = "手机号不能为空";
                } else {
                    gender = "男".equals(genderText) ? 1 : 0;
                    if (!entryDateText.isEmpty()) {
                        try {
                            entryDate = LocalDate.parse(entryDateText, dateFmt);
                        } catch (Exception e) {
                            err = "入职日期格式错误（应为 yyyy-MM-dd）：" + entryDateText;
                        }
                    }
                    if (err == null && !salaryText.isEmpty()) {
                        try {
                            salary = new BigDecimal(salaryText);
                        } catch (NumberFormatException e) {
                            err = "基本工资格式错误：" + salaryText;
                        }
                    }
                }

                if (err != null) {
                    Map<String, Object> fail = new HashMap<>();
                    fail.put("row", rowNo);
                    fail.put("empNo", empNo);
                    fail.put("name", name);
                    fail.put("reason", err);
                    errors.add(fail);
                    continue;
                }

                seenEmpNos.put(empNo, rowNo);
                Employee emp = new Employee();
                emp.setEmpNo(empNo);
                emp.setName(name);
                emp.setGender(gender);
                emp.setDeptId(deptId);
                emp.setPosition(position);
                emp.setPhone(phone);
                emp.setEmail(email.isEmpty() ? null : email);
                emp.setEntryDate(entryDate);
                emp.setSalary(salary);
                emp.setStatus(1);
                toInsert.add(emp);
            }

            int success = 0;
            for (Employee emp : toInsert) {
                try {
                    if (employeeService.save(emp)) {
                        success++;
                    } else {
                        errors.add(failRow(rows, toInsert.indexOf(emp), "入库失败"));
                    }
                } catch (Exception e) {
                    errors.add(failRow(rows, toInsert.indexOf(emp), "入库异常：" + e.getMessage()));
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("success", success);
            data.put("failed", errors.size());
            data.put("errors", errors);
            return Result.success("导入完成", data);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    private Map<String, Object> failRow(List<List<Object>> rows, int dataIndex, String reason) {
        List<Object> row = rows.get(dataIndex + 1);
        Map<String, Object> fail = new HashMap<>();
        fail.put("row", dataIndex + 2);
        fail.put("empNo", cell(row, 0));
        fail.put("name", cell(row, 1));
        fail.put("reason", reason);
        return fail;
    }

    private String cell(List<Object> row, int idx) {
        if (row == null || idx >= row.size() || row.get(idx) == null) {
            return "";
        }
        return String.valueOf(row.get(idx)).trim();
    }
}
