package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 招聘需求实体类
 *
 * 一条需求 = 某个部门要招几个人。hiredCount 在候选人录用时累加，
 * 累加到 headcount 后需求自动置为「已完成」，避免长期挂着招满的岗位。
 */
@Data
@TableName("hr_recruit_requisition")
public class JobRequisition implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：招聘中 */
    public static final int STATUS_OPEN = 1;
    /** 状态：已暂停 */
    public static final int STATUS_PAUSED = 2;
    /** 状态：已完成 */
    public static final int STATUS_DONE = 3;
    /** 状态：已关闭 */
    public static final int STATUS_CLOSED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 需求编号 REQ + 日期 + 三位序号 */
    private String reqNo;

    private String position;

    private Long deptId;

    /** 计划招聘人数 */
    private Integer headcount;

    /** 已录用人数 */
    private Integer hiredCount;

    /** 紧急程度 1一般 2紧急 3非常紧急 */
    private Integer urgency;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String education;

    private String experience;

    private String workLocation;

    private String jobDuty;

    private String requirement;

    private LocalDate publishDate;

    /** 期望到岗日期 */
    private LocalDate deadline;

    /** 需求提出人 */
    private String applicant;

    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 部门名称（列表展示用，非表字段，由 Service 批量填充） */
    @TableField(exist = false)
    private String deptName;

    /** 候选人总数（列表展示用，非表字段） */
    @TableField(exist = false)
    private Integer candidateCount;

    /** 在筛选流程中的候选人数：待筛选/初试/复试/待录用（阶段 1~4），非表字段 */
    @TableField(exist = false)
    private Integer activeCount;

    /** 已淘汰候选人数（阶段 6），非表字段 */
    @TableField(exist = false)
    private Integer rejectedCount;
}
