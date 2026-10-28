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
 * 线下交易台账实体类
 */
@Data
@TableName("biz_offline_deal")
public class OfflineDeal implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String dealNo;

    private Long customerId;

    private String customerName;

    private String customerCountry;

    private String itemName;

    private String currency;

    private BigDecimal amount;

    private LocalDate dealDate;

    /**
     * 收款方式 1银行转账 2现金 3微信 4支付宝 9其他
     */
    private Integer payMethod;

    private String voucher;

    /**
     * 0待确认 1已确认 9已取消
     */
    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
