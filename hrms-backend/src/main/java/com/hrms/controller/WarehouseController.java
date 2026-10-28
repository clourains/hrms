package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Inventory;
import com.hrms.entity.Warehouse;
import com.hrms.mapper.InventoryMapper;
import com.hrms.service.WarehouseService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private InventoryMapper inventoryMapper;

    /**
     * 仓库容量与地点可视化：返回每个仓库的容量、当前库存占用件数与使用率
     */
    @GetMapping("/overview")
    public Result<List<Map<String, Object>>> overview() {
        List<Warehouse> warehouses = warehouseService.listAll(false);
        List<Inventory> inventories = inventoryMapper.selectList(null);

        // 按仓库汇总库存（在库 + 锁定）
        Map<Long, Integer> stockMap = new HashMap<>();
        for (Inventory inv : inventories) {
            if (inv.getWarehouseId() == null) {
                continue;
            }
            int qty = (inv.getQuantity() == null ? 0 : inv.getQuantity())
                    + (inv.getLockedQuantity() == null ? 0 : inv.getLockedQuantity());
            stockMap.merge(inv.getWarehouseId(), qty, Integer::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Warehouse w : warehouses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", w.getId());
            item.put("warehouseCode", w.getWarehouseCode());
            item.put("warehouseName", w.getWarehouseName());
            item.put("type", w.getType());
            item.put("country", w.getCountry());
            item.put("city", w.getCity());
            item.put("address", w.getAddress());
            item.put("manager", w.getManager());
            item.put("capacity", w.getCapacity());
            item.put("status", w.getStatus());

            int stock = stockMap.getOrDefault(w.getId(), 0);
            int cap = w.getCapacity() == null ? 0 : w.getCapacity();
            item.put("stockQty", stock);
            item.put("usageRate", cap > 0 ? Math.round(stock * 1000.0 / cap) / 10.0 : 0.0);
            result.add(item);
        }
        return Result.success(result);
    }

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
