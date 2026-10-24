package com.hrms.controller;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockCheckOrderDTO;
import com.hrms.service.StockCheckService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 库存盘点单Controller（P019）
 */
@RestController
@RequestMapping("/stock-check")
public class StockCheckController {

    @Autowired
    private StockCheckService stockCheckService;

    /**
     * 建单时按仓库带出当前库存行
     */
    @GetMapping("/inventory-rows")
    public Result<Map<String, Object>> inventoryRows(@RequestParam Long warehouseId) {
        return Result.success(stockCheckService.inventoryRows(warehouseId));
    }

    @PostMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(stockCheckService.pageQuery(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(stockCheckService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody StockCheckOrderDTO dto) {
        return Result.success("盘点单已保存", stockCheckService.create(dto));
    }

    @PutMapping
    public Result<Void> update(@RequestBody StockCheckOrderDTO dto) {
        stockCheckService.update(dto);
        return Result.success("盘点单已更新", null);
    }

    @PostMapping("/{id}/confirm")
    public Result<Map<String, Object>> confirm(@PathVariable Long id) {
        return Result.success("盘点已确认", stockCheckService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        stockCheckService.cancel(id);
        return Result.success("盘点单已作废", null);
    }
}
