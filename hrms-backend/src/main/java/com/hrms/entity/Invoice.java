package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票记录实体类
 */
@Data
@TableName("biz_invoice")
public class Invoice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String invoiceNo;

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private String title;

    private String taxNo;

    private String currency;

    private BigDecimal amount;

    private String invoiceType;

    /**
     * 0待开票 1开票中 2已开票 3已作废
     */
    private Integer status;

    private LocalDate issueDate;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
