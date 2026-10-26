package com.hrms.controller;

import com.hrms.annotation.RequireRole;
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
 *
 * 业务模块整体仅管理员可访问（P045）
 */
@RequireRole("admin")
@RestController
@RequestMapping("/supplier")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @PostMapping("/page")
    public Result<PageResult<Supplier>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(supplierService.pageQuery(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<Supplier> getById(@PathVariable Long id) {
        return Result.success(supplierService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<Supplier>> list() {
        return Result.success(supplierService.listAll());
    }

    @PostMapping
    public Result<Void> save(@RequestBody Supplier supplier) {
        return supplierService.save(supplier) ? Result.success("新增成功", null) : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Supplier supplier) {
        return supplierService.update(supplier) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return supplierService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
