package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Shipment;

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
}
