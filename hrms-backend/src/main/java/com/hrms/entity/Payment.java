package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收付款实体类
 */
@Data
@TableName("biz_payment")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;

    private Long orderId;

    private Integer type;

    private BigDecimal amount;

    private String currency;

    private BigDecimal exchangeRate;

    private BigDecimal amountCny;

    private LocalDate paymentDate;

    private String method;

    private String remark;

    private LocalDateTime createTime;
}
