package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Shipment;
import com.hrms.entity.ShipmentTrack;
import com.hrms.service.ShipmentService;
import com.hrms.service.SeventeenTrackService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发货物流Controller
 *
 * 业务模块整体仅管理员可访问（P045）
 */
@RequireRole("admin")
@RestController
@RequestMapping("/shipment")
public class ShipmentController {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private SeventeenTrackService seventeenTrackService;

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

    /**
     * 第三方实时轨迹（17TRACK）：用发货单运单号实时查询，不落库。
     * 未配置 API Key / 运单号为空 / 第三方报错时返回明确错误信息。
     */
    @GetMapping("/{id}/liveTracks")
    public Result<Map<String, Object>> liveTracks(@PathVariable Long id) {
        Shipment shipment = shipmentService.getById(id);
        if (shipment == null) {
            return Result.error("发货单不存在");
        }
        try {
            List<Map<String, Object>> nodes = seventeenTrackService.query(shipment.getTrackingNo());
            Map<String, Object> data = new HashMap<>();
            data.put("source", "17TRACK");
            data.put("trackingNo", shipment.getTrackingNo());
            data.put("nodes", nodes);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 按任意运单号查 17TRACK 实时轨迹（不依赖发货单，便于用真实快递单号演示）
     */
    @GetMapping("/liveTrackByNo")
    public Result<Map<String, Object>> liveTrackByNo(@RequestParam String trackingNo) {
        try {
            List<Map<String, Object>> nodes = seventeenTrackService.query(trackingNo);
            Map<String, Object> data = new HashMap<>();
            data.put("source", "17TRACK");
            data.put("trackingNo", trackingNo);
            data.put("nodes", nodes);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
