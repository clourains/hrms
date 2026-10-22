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
 * 发货物流实体类
 */
@Data
@TableName("biz_shipment")
public class Shipment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String shipmentNo;

    private Long orderId;

    private LocalDate shipDate;

    private String transportType;

    private String carrier;

    private String trackingNo;

    private String containerNo;

    private String portFrom;

    private String portTo;

    private LocalDate etd;

    private LocalDate eta;

    private Integer status;

    private BigDecimal freightCostUsd;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
