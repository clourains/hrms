package com.hrms.controller;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Supplier;
import com.hrms.service.SupplierService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商Controller
 */
@RestController
@RequestMapping("/supplier")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @PostMapping("/page")
    public Result<PageResult<Supplier>> page(@RequestBody BizQueryDTO queryDTO) {
        try {
            return Result.success(supplierService.pageQuery(queryDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Supplier> getById(@PathVariable Long id) {
        try {
            return Result.success(supplierService.getById(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Supplier>> list() {
        try {
            return Result.success(supplierService.listAll());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<Void> save(@RequestBody Supplier supplier) {
        try {
            return supplierService.save(supplier) ? Result.success("新增成功", null) : Result.error("新增失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<Void> update(@RequestBody Supplier supplier) {
        try {
            return supplierService.update(supplier) ? Result.success("更新成功", null) : Result.error("更新失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            return supplierService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
