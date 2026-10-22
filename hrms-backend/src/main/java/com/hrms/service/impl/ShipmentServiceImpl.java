package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Shipment;
import com.hrms.entity.TradeOrder;
import com.hrms.mapper.ShipmentMapper;
import com.hrms.mapper.TradeOrderMapper;
import com.hrms.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 发货物流Service实现类
 */
@Service
public class ShipmentServiceImpl implements ShipmentService {

    @Autowired
    private ShipmentMapper shipmentMapper;

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Override
    public PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO) {
        Page<Shipment> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Shipment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Shipment::getShipmentNo, queryDTO.getKeyword())
                    .or().like(Shipment::getTrackingNo, queryDTO.getKeyword())
                    .or().like(Shipment::getContainerNo, queryDTO.getKeyword()));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Shipment::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getOrderId() != null) {
            wrapper.eq(Shipment::getOrderId, queryDTO.getOrderId());
        }
        wrapper.orderByDesc(Shipment::getCreateTime);
        Page<Shipment> result = shipmentMapper.selectPage(page, wrapper);

        List<Map<String, Object>> list = result.getRecords().stream().map(shipment -> {
            Map<String, Object> map = new HashMap<>();
            map.put("shipment", shipment);
            TradeOrder order = tradeOrderMapper.selectById(shipment.getOrderId());
            map.put("orderNo", order == null ? "" : order.getOrderNo());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public Shipment getById(Long id) {
        return shipmentMapper.selectById(id);
    }

    @Override
    public boolean save(Shipment shipment) {
        if (!StringUtils.hasText(shipment.getShipmentNo())) {
            shipment.setShipmentNo("SH" + System.currentTimeMillis());
        }
        shipment.setCreateTime(LocalDateTime.now());
        shipment.setUpdateTime(LocalDateTime.now());
        return shipmentMapper.insert(shipment) > 0;
    }

    @Override
    public boolean update(Shipment shipment) {
        shipment.setUpdateTime(LocalDateTime.now());
        return shipmentMapper.updateById(shipment) > 0;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        Shipment shipment = new Shipment();
        shipment.setId(id);
        shipment.setStatus(status);
        shipment.setUpdateTime(LocalDateTime.now());
        return shipmentMapper.updateById(shipment) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return shipmentMapper.deleteById(id) > 0;
    }
}
