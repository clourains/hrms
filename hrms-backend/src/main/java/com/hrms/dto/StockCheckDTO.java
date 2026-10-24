package com.hrms.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 单品盘点调整参数（P019 快捷盘点）
 *
 * 行内「盘点」按钮走这个：只盘一个 SKU，填实盘数后立即校准账面。
 * 正式的多品盘点走盘点单，见 {@link StockCheckOrderDTO}。
 */
@Data
public class StockCheckDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;

    private Long warehouseId;

    private Integer actualQuantity;

    private String remark;
}
