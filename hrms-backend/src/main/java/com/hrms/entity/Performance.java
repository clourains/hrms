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
 * 绩效实体类
 */
@Data
@TableName("sys_performance")
public class Performance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String period;

    private BigDecimal workQuality;

    private BigDecimal workEfficiency;

    private BigDecimal workAttitude;

    private BigDecimal teamwork;

    private BigDecimal innovation;

    private BigDecimal totalScore;

    private String grade;

    private String evaluator;

    private String comment;

    private Integer status;

    private BigDecimal selfScore;

    private BigDecimal managerScore;

    private String selfSummary;

    private String improvementPlan;

    private String interviewNote;

    private LocalDateTime interviewTime;

    private String flowStatus;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
