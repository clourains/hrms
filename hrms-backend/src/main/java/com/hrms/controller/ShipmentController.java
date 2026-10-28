package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Shipment;
import com.hrms.entity.ShipmentTrack;
import com.hrms.service.ShipmentService;
import com.hrms.service.SeventeenTrackService;
import com.hrms.util.ExcelExportUtil;
import com.hrms.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * 驾驶舱航线：只返回已有真实轨迹节点的在途/签收单，前端据此画线。
     * 没有节点的单不会出现，避免固定进度假航线。
     * keyword 支持发货单号/运单号/承运商/集装箱号/起运港/目的港/订单号/客户名称/货物名称。
     */
    @GetMapping("/map-routes")
    public Result<List<Map<String, Object>>> mapRoutes(
            @RequestParam(required = false) String keyword) {
        return Result.success(shipmentService.mapRoutes(keyword));
    }

    /**
     * 把 17TRACK 实时节点落库（去重），返回本次新增节点数。
     * 同步后驾驶舱地图与轨迹导出都会用上详细节点。
     */
    @PostMapping("/{id}/sync-tracks")
    public Result<Map<String, Object>> syncTracks(@PathVariable Long id) {
        try {
            int inserted = shipmentService.syncLiveTracks(id);
            Map<String, Object> data = new HashMap<>();
            data.put("inserted", inserted);
            data.put("total", shipmentService.listTracks(id).size());
            return Result.success("同步完成", data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 单票物流轨迹导出：发货单信息 + 全部节点（时间/节点/地点/描述/来源）
     */
    @GetMapping("/{id}/tracks/export")
    public void exportTracks(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Shipment shipment = shipmentService.getById(id);
        if (shipment == null) {
            throw new RuntimeException("发货单不存在");
        }
        List<ShipmentTrack> tracks = shipmentService.listTracks(id);
        List<List<Object>> rows = new ArrayList<>();
        List<Object> headInfo = new ArrayList<>();
        headInfo.add("发货单号");
        headInfo.add(shipment.getShipmentNo());
        rows.add(headInfo);
        rows.add(java.util.Arrays.asList("追踪号", shipment.getTrackingNo(), "承运商", shipment.getCarrier()));
        rows.add(java.util.Arrays.asList("起运港 → 目的港",
                (shipment.getPortFrom() == null ? "" : shipment.getPortFrom()) + " → "
                        + (shipment.getPortTo() == null ? "" : shipment.getPortTo())));
        rows.add(java.util.Arrays.asList("状态", statusText(shipment.getStatus()),
                "节点数", tracks.size()));
        rows.add(Collections.emptyList());
        rows.add(java.util.Arrays.asList("序号", "时间", "节点", "地点", "描述", "来源"));
        int index = 1;
        for (ShipmentTrack t : tracks) {
            rows.add(java.util.Arrays.asList(index++, t.getTrackTime(), t.getNode(), t.getLocation(),
                    t.getContent(), t.getOperator()));
        }
        String name = "物流轨迹_" + (shipment.getTrackingNo() == null ? shipment.getShipmentNo() : shipment.getTrackingNo());
        ExcelExportUtil.write(response, name, null, rows);
    }

    private String statusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "待发货";
            case 2 -> "运输中";
            case 3 -> "清关中";
            case 4 -> "已签收";
            default -> "未知";
        };
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
