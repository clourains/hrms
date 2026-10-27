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
 * 招聘候选人实体类
 *
 * stage 是候选人当前所处环节，由「面试结果」和「录用」动作自动推进：
 * 初试通过 -> 3复试，复试通过 -> 4待录用，录用 -> 5已录用，任一轮不通过 -> 6已淘汰。
 */
@Data
@TableName("hr_recruit_candidate")
public class Candidate implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 阶段：待筛选 */
    public static final int STAGE_SCREENING = 1;
    /** 阶段：初试 */
    public static final int STAGE_FIRST = 2;
    /** 阶段：复试 */
    public static final int STAGE_SECOND = 3;
    /** 阶段：待录用 */
    public static final int STAGE_OFFER = 4;
    /** 阶段：已录用 */
    public static final int STAGE_HIRED = 5;
    /** 阶段：已淘汰 */
    public static final int STAGE_REJECTED = 6;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 候选人编号 C + 日期 + 三位序号 */
    private String candidateNo;

    private Long requisitionId;

    private String name;

    /** 性别 0女 1男 */
    private Integer gender;

    private LocalDate birthday;

    private String phone;

    private String email;

    private String education;

    private String school;

    private String major;

    private Integer workYears;

    private String currentCompany;

    private String currentPosition;

    private BigDecimal expectSalary;

    /** 来源：简历投递/内部推荐/猎头/招聘网站/校园招聘 */
    private String source;

    private Integer stage;

    /** 录用后生成的员工ID */
    private Long employeeId;

    private String rejectReason;

    private String remark;

    /** 简历文件相对路径（相对 hrms.upload.dir，如 resume/2026/10/xxx.jpg） */
    private String resumeUrl;

    /** 简历原始文件名，下载时还原 */
    private String resumeName;

    /** 简历文件大小（字节） */
    private Long resumeSize;

    /** 简历归类：image / pdf */
    private String resumeType;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 应聘岗位（非表字段，取关联招聘需求） */
    @TableField(exist = false)
    private String requisitionPosition;

    /** 需求部门名称（非表字段） */
    @TableField(exist = false)
    private String deptName;

    /** 面试轮次数（非表字段，列表展示"已面 2 轮"） */
    @TableField(exist = false)
    private Integer interviewCount;
}
