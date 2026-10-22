package com.hrms.controller;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockChangeDTO;
import com.hrms.entity.InventoryLog;
import com.hrms.service.InventoryService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 库存Controller
 */
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestBody BizQueryDTO queryDTO) {
        try {
            return Result.success(inventoryService.pageQuery(queryDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/logs")
    public Result<PageResult<InventoryLog>> logs(@RequestBody BizQueryDTO queryDTO) {
        try {
            return Result.success(inventoryService.logPageQuery(queryDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/change")
    public Result<Void> change(@RequestBody StockChangeDTO dto) {
        try {
            return inventoryService.changeStock(dto) ? Result.success("操作成功", null) : Result.error("操作失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/warning")
    public Result<List<Map<String, Object>>> warning() {
        try {
            return Result.success(inventoryService.warningList());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
