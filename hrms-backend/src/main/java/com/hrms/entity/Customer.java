package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 境外客户实体类
 */
@Data
@TableName("biz_customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String customerCode;

    private String companyName;

    private String contactName;

    private String country;

    private String city;

    private String address;

    private String email;

    private String phone;

    private String website;

    private String level;

    private BigDecimal creditLimitUsd;

    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
