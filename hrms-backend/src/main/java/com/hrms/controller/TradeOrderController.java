package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.OrderCreateDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.OrderItem;
import com.hrms.entity.TradeOrder;
import com.hrms.service.TradeOrderService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 销售订单Controller
 *
 * 业务模块整体仅管理员可访问（P045）
 */
@RequireRole("admin")
@RestController
@RequestMapping("/order")
public class TradeOrderController {

    @Autowired
    private TradeOrderService tradeOrderService;

    @PostMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(tradeOrderService.pageQuery(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<TradeOrder> getById(@PathVariable Long id) {
        return Result.success(tradeOrderService.getById(id));
    }

    @GetMapping("/items/{orderId}")
    public Result<List<OrderItem>> items(@PathVariable Long orderId) {
        return Result.success(tradeOrderService.getItems(orderId));
    }

    @PostMapping
    public Result<TradeOrder> create(@RequestBody OrderCreateDTO dto) {
        TradeOrder order = tradeOrderService.createOrder(dto);
        return Result.success("创建成功", order);
    }

    @PutMapping
    public Result<Void> update(@RequestBody TradeOrder order) {
        return tradeOrderService.update(order) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @PutMapping("/status/{id}/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        return tradeOrderService.updateStatus(id, status) ? Result.success("操作成功", null) : Result.error("操作失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return tradeOrderService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
