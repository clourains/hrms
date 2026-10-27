package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 试用期转正实体类
 *
 * 候选人录用后自动生成一条「试用中」记录，试用期结束前由这里办理转正/延期/不予转正。
 * 转正只改 sys_employee.status（2试用期 -> 1在职）和本人的 status/confirmDate，
 * 不给员工表加新的列，避免与其他人对 sys_employee 的改动冲突。
 */
@Data
@TableName("hr_recruit_probation")
public class Probation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：试用中 */
    public static final int STATUS_PROBATION = 1;
    /** 状态：已转正 */
    public static final int STATUS_REGULAR = 2;
    /** 状态：未通过 */
    public static final int STATUS_FAILED = 3;
    /** 状态：已延期 */
    public static final int STATUS_EXTENDED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    /** 来源候选人ID，手工登记的试用期为空 */
    private Long candidateId;

    private Long deptId;

    private String position;

    private LocalDate probationStart;

    private LocalDate probationEnd;

    /** 带教导师 */
    private String mentor;

    private Integer status;

    /** 实际转正/终止日期 */
    private LocalDate confirmDate;

    private Integer score;

    private String evaluation;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 员工姓名（非表字段） */
    @TableField(exist = false)
    private String employeeName;

    /** 工号（非表字段） */
    @TableField(exist = false)
    private String empNo;

    /** 部门名称（非表字段） */
    @TableField(exist = false)
    private String deptName;

    /**
     * 距离试用期结束还有多少天（非表字段）
     * 负数表示已逾期未办理，0 表示今天到期。前端据此标红/标黄。
     */
    @TableField(exist = false)
    private Long daysLeft;
}
