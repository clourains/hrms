package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存盘点单明细（P019）
 */
@Data
@TableName("biz_stock_check_item")
public class StockCheckItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long checkId;

    private Long productId;

    /**
     * 账面数量（建单时是快照，确认时会被刷新成确认那一刻的实际值）
     */
    private Integer bookQuantity;

    /**
     * 实盘数量
     */
    private Integer actualQuantity;

    /**
     * 差异 = 实盘 - 账面
     */
    private Integer diff;

    private String remark;
}
