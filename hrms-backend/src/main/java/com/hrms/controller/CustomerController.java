package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Customer;
import com.hrms.service.CustomerService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 境外客户Controller
 *
 * 业务模块整体仅管理员可访问（P045）
 */
@RequireRole("admin")
@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/page")
    public Result<PageResult<Customer>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(customerService.pageQuery(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable Long id) {
        return Result.success(customerService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<Customer>> list() {
        return Result.success(customerService.listAll());
    }

    @PostMapping
    public Result<Void> save(@RequestBody Customer customer) {
        return customerService.save(customer) ? Result.success("新增成功", null) : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Customer customer) {
        return customerService.update(customer) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return customerService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
