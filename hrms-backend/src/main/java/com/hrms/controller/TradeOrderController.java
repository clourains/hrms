package com.hrms.controller;

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
 */
@RestController
@RequestMapping("/order")
public class TradeOrderController {

    @Autowired
    private TradeOrderService tradeOrderService;

    @PostMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestBody BizQueryDTO queryDTO) {
        try {
            return Result.success(tradeOrderService.pageQuery(queryDTO));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<TradeOrder> getById(@PathVariable Long id) {
        try {
            return Result.success(tradeOrderService.getById(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/items/{orderId}")
    public Result<List<OrderItem>> items(@PathVariable Long orderId) {
        try {
            return Result.success(tradeOrderService.getItems(orderId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<TradeOrder> create(@RequestBody OrderCreateDTO dto) {
        try {
            TradeOrder order = tradeOrderService.createOrder(dto);
            return Result.success("创建成功", order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<Void> update(@RequestBody TradeOrder order) {
        try {
            return tradeOrderService.update(order) ? Result.success("更新成功", null) : Result.error("更新失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/status/{id}/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        try {
            return tradeOrderService.updateStatus(id, status) ? Result.success("操作成功", null) : Result.error("操作失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            return tradeOrderService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
