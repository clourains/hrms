package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Shipment;
import com.hrms.entity.ShipmentTrack;

import java.util.List;
import java.util.Map;

/**
 * 发货物流Service接口
 */
public interface ShipmentService {

    PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO);

    Shipment getById(Long id);

    boolean save(Shipment shipment);

    boolean update(Shipment shipment);

    boolean updateStatus(Long id, Integer status);

    boolean deleteById(Long id);

    /**
     * 物流轨迹节点列表（P018）
     */
    List<ShipmentTrack> listTracks(Long shipmentId);

    /**
     * 驾驶舱航线用的真实轨迹摘要：只返回已有轨迹节点的在途/签收发货单。
     * 前端据此画线，节点不足的单不会被画出来，避免出现假航线。
     *
     * @param keyword 可选搜索词：发货单号 / 运单号 / 承运商 / 集装箱号 / 起运港 / 目的港 /
     *                订单号 / 客户名称 / 货物（产品）名称，为空则返回全部
     */
    List<Map<String, Object>> mapRoutes(String keyword);

    /**
     * 把 17TRACK 实时节点落库（去重），同步后地图与导出才能用上详细轨迹。
     * 返回本次新增节点数。
     */
    int syncLiveTracks(Long shipmentId);
}
