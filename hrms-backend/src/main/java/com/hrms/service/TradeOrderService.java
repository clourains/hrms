package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.OrderCreateDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.OrderItem;
import com.hrms.entity.TradeOrder;

import java.util.List;
import java.util.Map;

/**
 * 销售订单Service接口
 */
public interface TradeOrderService {

    PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO);

    TradeOrder getById(Long id);

    List<OrderItem> getItems(Long orderId);

    /**
     * 创建订单（含明细，生成订单号，锁定库存）
     */
    TradeOrder createOrder(OrderCreateDTO dto);

    /**
     * 更新订单状态
     */
    boolean updateStatus(Long id, Integer status);

    boolean update(TradeOrder order);

    boolean deleteById(Long id);
}
