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
 * 薪资实体类
 */
@Data
@TableName("sys_salary")
public class Salary implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String salaryMonth;

    private BigDecimal baseSalary;

    private BigDecimal performanceBonus;

    private BigDecimal overtimePay;

    private BigDecimal allowance;

    private BigDecimal socialSecurity;

    private BigDecimal housingFund;

    private BigDecimal tax;

    private BigDecimal otherDeduction;

    private BigDecimal netSalary;

    private Integer status;

    private LocalDateTime payTime;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
