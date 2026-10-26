package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批节点记录实体（P3-1）
 */
@Data
@TableName("sys_approval_record")
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    /** 节点序号（1起） */
    private Integer nodeOrder;

    /** 审批人用户名 */
    private String approver;

    /** 审批人姓名冗余 */
    private String approverName;

    /** 0待审 1通过 2驳回 */
    private Integer status;

    /** 审批意见 */
    private String comment;

    private LocalDateTime approveTime;

    private LocalDateTime createTime;
}
