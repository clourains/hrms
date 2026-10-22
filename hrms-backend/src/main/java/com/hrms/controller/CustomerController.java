package com.hrms.controller;

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
 */
@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/page")
    public Result<PageResult<Customer>> page(@RequestBody BizQueryDTO queryDTO) {
        try {
            return Result.success(customerService.pageQuery(queryDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable Long id) {
        try {
            return Result.success(customerService.getById(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Customer>> list() {
        try {
            return Result.success(customerService.listAll());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<Void> save(@RequestBody Customer customer) {
        try {
            return customerService.save(customer) ? Result.success("新增成功", null) : Result.error("新增失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<Void> update(@RequestBody Customer customer) {
        try {
            return customerService.update(customer) ? Result.success("更新成功", null) : Result.error("更新失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            return customerService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
