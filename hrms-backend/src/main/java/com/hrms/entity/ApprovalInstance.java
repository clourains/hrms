package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批实例实体（P3-1）
 */
@Data
@TableName("sys_approval_instance")
public class ApprovalInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型：请假/调薪/通用 */
    private String bizType;

    private String title;

    /** 申请内容 JSON（请假：startDate/endDate/days/reason） */
    private String detail;

    /** 申请人用户名 */
    private String applicant;

    /** 申请人姓名冗余 */
    private String applicantName;

    /** 0审批中 1已通过 2已驳回 3已撤销 */
    private Integer status;

    /** 当前节点序号（1起） */
    private Integer currentNode;

    /** 审批级数 */
    private Integer nodeCount;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
