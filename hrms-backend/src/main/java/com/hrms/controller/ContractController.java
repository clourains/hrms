package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Contract;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.mapper.ContractMapper;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工合同台账 + 到期提醒 Controller（P1-2）
 *
 * 签证信息在 sys_employee（passport_no/visa_type/visa_expire_date），
 * 合同信息在本控制器维护的 sys_contract，临期扫描统一从这里出。
 */
@RestController
@RequestMapping("/contract")
public class ContractController {

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    /**
     * 合同列表（可按员工过滤），附带员工姓名/部门名便于台账展示
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long empId) {
        try {
            List<Contract> contracts = contractMapper.selectList(
                    new LambdaQueryWrapper<Contract>()
                            .eq(empId != null, Contract::getEmpId, empId)
                            .orderByAsc(Contract::getEndDate));
            return Result.success(fillNames(contracts));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增合同
     */
    @RequireRole("admin")
    @AuditLog(module = "合同管理", action = "新增")
    @PostMapping
    public Result<Void> save(@RequestBody Contract contract) {
        try {
            if (contract.getEndDate().isBefore(contract.getStartDate())) {
                return Result.error("合同结束日期不能早于开始日期");
            }
            if (contract.getStatus() == null) {
                contract.setStatus(1);
            }
            contract.setId(null);
            contractMapper.insert(contract);
            return Result.success("新增成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改合同
     */
    @RequireRole("admin")
    @AuditLog(module = "合同管理", action = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody Contract contract) {
        try {
            if (contract.getId() == null) {
                return Result.error("缺少合同ID");
            }
            if (contract.getEndDate() != null && contract.getStartDate() != null
                    && contract.getEndDate().isBefore(contract.getStartDate())) {
                return Result.error("合同结束日期不能早于开始日期");
            }
            contractMapper.updateById(contract);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除合同
     */
    @RequireRole("admin")
    @AuditLog(module = "合同管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            contractMapper.deleteById(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 到期提醒扫描：合同与签证在 days 天内到期的记录（默认 60 天）
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/expiring")
    public Result<Map<String, Object>> expiring(@RequestParam(defaultValue = "60") int days) {
        try {
            int range = Math.max(1, Math.min(days, 365));
            LocalDate today = LocalDate.now();
            LocalDate deadline = today.plusDays(range);

            // 员工/部门名称映射
            List<Employee> employees = employeeMapper.selectList(null);
            Map<Long, Employee> empMap = employees.stream()
                    .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
            List<Long> deptIds = employees.stream()
                    .map(Employee::getDeptId).filter(Objects::nonNull).distinct().toList();
            Map<Long, String> deptMap = deptIds.isEmpty() ? Collections.emptyMap()
                    : departmentMapper.selectBatchIds(deptIds).stream()
                            .collect(Collectors.toMap(Department::getId, Department::getDeptName));

            // 1) 有效合同在 [today - range, deadline] 内到期
            //    下界往前放宽 range 天是为了把「已经过期」的合同也带出来 ——
            //    原先只扫 [today, deadline]，合同一过期就从提醒里消失，
            //    「越过期越看不见」，日常使用中很容易漏掉续签
            List<Map<String, Object>> contractAlerts = new ArrayList<>();
            List<Contract> contracts = contractMapper.selectList(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getStatus, 1)
                            .ge(Contract::getEndDate, today.minusDays(range))
                            .le(Contract::getEndDate, deadline)
                            .orderByAsc(Contract::getEndDate));
            for (Contract c : contracts) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", c.getId());
                item.put("empId", c.getEmpId());
                Employee emp = empMap.get(c.getEmpId());
                item.put("empName", emp != null ? emp.getName() : "未知员工(" + c.getEmpId() + ")");
                item.put("deptName", emp != null && emp.getDeptId() != null
                        ? deptMap.get(emp.getDeptId()) : null);
                item.put("contractNo", c.getContractNo());
                item.put("contractType", c.getContractType());
                item.put("startDate", c.getStartDate());
                item.put("endDate", c.getEndDate());
                item.put("daysLeft", ChronoUnit.DAYS.between(today, c.getEndDate()));
                // 到期提醒页的编辑弹窗用 row.remark / row.status 回填，
                // 原先这里不带这两个字段，回填成空后一保存就把库里的备注覆盖了
                item.put("remark", c.getRemark());
                item.put("status", c.getStatus());
                contractAlerts.add(item);
            }

            // 2) 在职/试用员工签证在 [today, deadline] 内到期（离职员工不提醒）
            List<Map<String, Object>> visaAlerts = new ArrayList<>();
            for (Employee emp : employees) {
                if (emp.getStatus() != null && emp.getStatus() == 0) {
                    continue;
                }
                if (emp.getVisaExpireDate() == null) {
                    continue;
                }
                if (!emp.getVisaExpireDate().isBefore(today)
                        && !emp.getVisaExpireDate().isAfter(deadline)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("empId", emp.getId());
                    item.put("empName", emp.getName());
                    item.put("deptName", emp.getDeptId() != null ? deptMap.get(emp.getDeptId()) : null);
                    item.put("passportNo", emp.getPassportNo());
                    item.put("visaType", emp.getVisaType());
                    item.put("visaExpireDate", emp.getVisaExpireDate());
                    item.put("daysLeft", ChronoUnit.DAYS.between(today, emp.getVisaExpireDate()));
                    visaAlerts.add(item);
                }
            }
            visaAlerts.sort(Comparator.comparingLong(m -> (Long) m.get("daysLeft")));

            Map<String, Object> data = new HashMap<>();
            data.put("contractAlerts", contractAlerts);
            data.put("visaAlerts", visaAlerts);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 列表附带员工姓名/部门名
     */
    private List<Map<String, Object>> fillNames(List<Contract> contracts) {
        List<Long> empIds = contracts.stream().map(Contract::getEmpId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Employee> empMap = empIds.isEmpty() ? Collections.emptyMap()
                : employeeMapper.selectBatchIds(empIds).stream()
                        .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        List<Long> deptIds = empMap.values().stream()
                .map(Employee::getDeptId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> deptMap = deptIds.isEmpty() ? Collections.emptyMap()
                : departmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Contract c : contracts) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("empId", c.getEmpId());
            Employee emp = empMap.get(c.getEmpId());
            row.put("empName", emp != null ? emp.getName() : null);
            row.put("deptName", emp != null && emp.getDeptId() != null
                    ? deptMap.get(emp.getDeptId()) : null);
            row.put("contractNo", c.getContractNo());
            row.put("contractType", c.getContractType());
            row.put("startDate", c.getStartDate());
            row.put("endDate", c.getEndDate());
            row.put("status", c.getStatus());
            row.put("remark", c.getRemark());
            rows.add(row);
        }
        return rows;
    }
}
