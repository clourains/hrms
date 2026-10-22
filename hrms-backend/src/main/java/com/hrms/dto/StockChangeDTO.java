package com.hrms.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 库存变动DTO
 */
@Data
public class StockChangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;

    private Long warehouseId;

    /**
     * 类型 1入库 2出库 3锁定 4解锁
     */
    private Integer changeType;

    private Integer quantity;

    private String orderNo;

    private String operator;

    private String remark;
}
