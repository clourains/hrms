package com.hrms.controller;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Shipment;
import com.hrms.entity.ShipmentTrack;
import com.hrms.service.ShipmentService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 发货物流Controller
 */
@RestController
@RequestMapping("/shipment")
public class ShipmentController {

    @Autowired
    private ShipmentService shipmentService;

    @PostMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(shipmentService.pageQuery(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<Shipment> getById(@PathVariable Long id) {
        return Result.success(shipmentService.getById(id));
    }

    /**
     * 物流轨迹节点（P018）
     */
    @GetMapping("/{id}/tracks")
    public Result<List<ShipmentTrack>> tracks(@PathVariable Long id) {
        return Result.success(shipmentService.listTracks(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Shipment shipment) {
        return shipmentService.save(shipment) ? Result.success("新增成功", null) : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Shipment shipment) {
        return shipmentService.update(shipment) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @PutMapping("/status/{id}/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        return shipmentService.updateStatus(id, status) ? Result.success("操作成功", null) : Result.error("操作失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return shipmentService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
