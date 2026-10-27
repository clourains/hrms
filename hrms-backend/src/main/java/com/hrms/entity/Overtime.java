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
 * 加班申请实体类（P4-1）
 *
 * 发起时同步创建通用审批单（biz_type=加班），审批全部通过后 status 置 1，
 * 薪资计算引擎按当月已通过的加班时长计加班费。
 */
@Data
@TableName("sys_overtime")
public class Overtime implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private LocalDate overtimeDate;

    private BigDecimal hours;

    private String reason;

    /** 0审批中 1已通过 2已驳回 3已撤销 */
    private Integer status;

    private Long instanceId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
