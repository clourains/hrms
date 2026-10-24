package com.hrms.controller;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockChangeDTO;
import com.hrms.dto.StockCheckDTO;
import com.hrms.dto.StockTransferDTO;
import com.hrms.service.InventoryService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 库存Controller
 *
 * 异常统一交给 GlobalExceptionHandler 处理，这里不再逐个 catch 后把 SQL 报错原样返回（P024）。
 */
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(inventoryService.pageQuery(queryDTO));
    }

    @PostMapping("/logs")
    public Result<PageResult<Map<String, Object>>> logs(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(inventoryService.logPageQuery(queryDTO));
    }

    @PostMapping("/change")
    public Result<Void> change(@RequestBody StockChangeDTO dto) {
        return inventoryService.changeStock(dto) ? Result.success("操作成功", null) : Result.error("操作失败");
    }

    @GetMapping("/warning")
    public Result<List<Map<String, Object>>> warning() {
        return Result.success(inventoryService.warningList());
    }

    /**
     * 顶部通知红点用：只返回条数（P022）
     */
    @GetMapping("/warning/count")
    public Result<Integer> warningCount() {
        return Result.success(inventoryService.warningCount());
    }

    /**
     * 修改预警值（P035）
     */
    @PutMapping("/{id}/warning")
    public Result<Void> updateWarning(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer value = body == null ? null : body.get("warningQuantity");
        return inventoryService.updateWarningQuantity(id, value)
                ? Result.success("预警值已更新", null) : Result.error("更新失败");
    }

    /**
     * 仓间调拨（P019）
     */
    @PostMapping("/transfer")
    public Result<Void> transfer(@RequestBody StockTransferDTO dto) {
        return inventoryService.transfer(dto.getProductId(), dto.getFromWarehouseId(),
                dto.getToWarehouseId(), dto.getQuantity(), dto.getRemark())
                ? Result.success("调拨成功", null) : Result.error("调拨失败");
    }

    /**
     * 盘点调整（P019）
     */
    @PostMapping("/check")
    public Result<Map<String, Object>> check(@RequestBody StockCheckDTO dto) {
        return Result.success("盘点完成", inventoryService.checkStock(dto.getProductId(),
                dto.getWarehouseId(), dto.getActualQuantity(), dto.getRemark()));
    }
}
