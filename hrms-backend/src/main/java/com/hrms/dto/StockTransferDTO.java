package com.hrms.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 仓间调拨参数（P019）
 */
@Data
public class StockTransferDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;

    private Long fromWarehouseId;

    private Long toWarehouseId;

    private Integer quantity;

    private String remark;
}
