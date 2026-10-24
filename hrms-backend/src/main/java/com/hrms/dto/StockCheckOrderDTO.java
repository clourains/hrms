package com.hrms.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 库存盘点单提交参数（P019）
 *
 * 一张单可带多行明细；实盘数量传 null 表示"该行未盘到，按账面一致"。
 */
@Data
public class StockCheckOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 单ID，修改草稿时必填
     */
    private Long id;

    private Long warehouseId;

    private LocalDate checkDate;

    private String remark;

    private List<Item> items;

    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long productId;

        /**
         * 实盘数量；null 表示未盘到，按账面一致处理
         */
        private Integer actualQuantity;

        private String remark;
    }
}
