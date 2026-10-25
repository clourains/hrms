package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.EmployeeQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Employee;
import com.hrms.service.EmployeeService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工Controller
 */
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

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
}
