package com.hrms.dto;

import lombok.Data;

/**
 * 招聘模块通用查询DTO
 *
 * 招聘四个列表的筛选条件差异较大（需求按部门/紧急度、候选人按阶段/需求、
 * 面试按结果、试用期按到期），直接复用 BizQueryDTO 会缺字段，
 * 因此在它基础上扩展，分页与排序参数沿用父类（含页码/页大小的边界保护）。
 */
@Data
public class RecruitQueryDTO extends BizQueryDTO {

    private static final long serialVersionUID = 1L;

    /** 招聘需求ID（候选人、面试按需求过滤） */
    private Long requisitionId;

    /** 候选人ID（面试按候选人过滤） */
    private Long candidateId;

    /** 部门ID */
    private Long deptId;

    /** 候选人阶段 1待筛选 2初试 3复试 4待录用 5已录用 6已淘汰 */
    private Integer stage;

    /** 面试结果 0待面试 1通过 2未通过 3爽约 */
    private Integer result;

    /** 紧急程度 1一般 2紧急 3非常紧急 */
    private Integer urgency;

    /** 面试轮次 1初试 2复试 3终试 */
    private Integer round;

    /** 只看 N 天内到期/已逾期的试用期（试用期页提醒用） */
    private Integer expireDays;

    /** 候选人来源渠道（简历投递/内部推荐/猎头/招聘网站/校园招聘） */
    private String source;
}
