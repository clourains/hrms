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
}
