package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假额度实体类（P4-2）
 *
 * 按员工+年度+类型唯一；请假审批通过后由审批流回调扣减 used_days。
 */
@Data
@TableName("sys_leave_quota")
public class LeaveQuota implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private Integer year;

    private String quotaType;

    private BigDecimal totalDays;

    private BigDecimal usedDays;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
