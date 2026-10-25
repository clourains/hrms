package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Warehouse;
import com.hrms.service.WarehouseService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仓库Controller
 *
 * 业务模块整体仅管理员可访问（P045）
 */
@RequireRole("admin")
@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @PostMapping("/page")
    public Result<PageResult<Warehouse>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(warehouseService.pageQuery(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<Warehouse> getById(@PathVariable Long id) {
        return Result.success(warehouseService.getById(id));
    }

    /**
     * 仓库列表
     *
     * @param onlyEnabled 默认 true，只返回启用的仓库（供下拉框使用）；
     *                    仓库管理页传 false，返回全部状态，停用的也能看到并改回启用
     */
    @GetMapping("/list")
    public Result<List<Warehouse>> list(@RequestParam(value = "onlyEnabled", required = false) Boolean onlyEnabled) {
        boolean only = onlyEnabled == null || onlyEnabled;
        return Result.success(warehouseService.listAll(only));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Warehouse warehouse) {
        return warehouseService.save(warehouse) ? Result.success("新增成功", null) : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Warehouse warehouse) {
        return warehouseService.update(warehouse) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return warehouseService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
