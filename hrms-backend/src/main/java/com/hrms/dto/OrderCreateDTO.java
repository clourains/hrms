package com.hrms.dto;

import com.hrms.entity.OrderItem;
import com.hrms.entity.TradeOrder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单创建DTO（订单头 + 明细行）
 */
@Data
public class OrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单头信息
     */
    private TradeOrder order;

    /**
     * 订单明细
     */
    private List<OrderItem> items;
}
