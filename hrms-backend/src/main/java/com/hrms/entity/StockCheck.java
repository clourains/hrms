package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存盘点单（P019）
 *
 * 原来只能对单个 SKU 做即时盘点，填完立刻改账、无单号、无法追溯。
 * 改成单据后：一张单可盘多个 SKU，草稿可改，确认时在一个事务里批量调整库存并写流水。
 */
@Data
@TableName("biz_stock_check")
public class StockCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态：草稿
     */
    public static final int STATUS_DRAFT = 1;

    /**
     * 状态：已确认（库存已按差异调整）
     */
    public static final int STATUS_CONFIRMED = 2;

    /**
     * 状态：已作废（仅草稿可作废，不影响库存）
     */
    public static final int STATUS_CANCELLED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 盘点单号，如 PD20261024-0001
     */
    private String checkNo;

    private Long warehouseId;

    private LocalDate checkDate;

    /**
     * 1草稿 2已确认 3已作废
     */
    private Integer status;

    /**
     * 明细行数
     */
    private Integer itemCount;

    /**
     * 有差异的行数
     */
    private Integer diffRows;

    /**
     * 差异合计，盘盈为正、盘亏为负
     */
    private Integer diffSum;

    private String operator;

    private String remark;

    private LocalDateTime confirmTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
