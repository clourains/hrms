package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 面试安排实体类
 *
 * 一场面试对应一条记录，候选人可有多条（初试/复试/终试）。
 * result 登记后由 Service 自动推进候选人阶段，不需要手工改候选人。
 */
@Data
@TableName("hr_recruit_interview")
public class CandidateInterview implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 结果：待面试 */
    public static final int RESULT_PENDING = 0;
    /** 结果：通过 */
    public static final int RESULT_PASS = 1;
    /** 结果：未通过 */
    public static final int RESULT_FAIL = 2;
    /** 结果：候选人爽约 */
    public static final int RESULT_ABSENT = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long candidateId;

    /** 冗余招聘需求ID，列表按需求筛选时不用再关联候选人 */
    private Long requisitionId;

    /** 轮次 1初试 2复试 3终试 */
    private Integer round;

    /** 形式 1现场 2电话 3视频 */
    private Integer interviewType;

    /**
     * 面试时间
     *
     * 项目没有全局 Jackson 时间配置，LocalDateTime 默认只认 ISO 的 2026-10-28T10:00:00，
     * 而前端日期控件统一输出「2026-10-28 10:00:00」，不显式声明格式会直接反序列化失败
     * （报"提交的数据格式不正确"）。这里前后端统一用空格分隔的格式。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime interviewTime;

    private Integer duration;

    /** 面试官，多人用逗号分隔 */
    private String interviewer;

    private String location;

    private Integer result;

    private Integer score;

    private String comment;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 候选人姓名（非表字段） */
    @TableField(exist = false)
    private String candidateName;

    /** 候选人手机号（非表字段，面试当天联系用） */
    @TableField(exist = false)
    private String candidatePhone;

    /** 应聘岗位（非表字段） */
    @TableField(exist = false)
    private String requisitionPosition;

    /** 候选人当前阶段（非表字段，列表里直接看进度） */
    @TableField(exist = false)
    private Integer candidateStage;
}
