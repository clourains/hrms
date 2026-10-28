package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工入职登记（入离管理的「入」）
 *
 * 先登记资料，再关联员工档案：employeeId 为空表示还没落档案，
 * 可由 HR「关联已有档案」或「一键新建档案」完成落档。
 */
@Data
@TableName("hr_employee_onboard")
public class EmployeeOnboard implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 0 待关联档案 */
    public static final int STATUS_UNLINKED = 0;
    /** 1 已关联档案 */
    public static final int STATUS_LINKED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登记编号 RD + 时间戳 */
    private String onboardNo;

    /** 关联的员工档案ID，为空表示待关联 */
    private Long employeeId;

    private String name;

    /** 1男 2女 */
    private Integer gender;

    private String phone;

    private Long deptId;

    private String deptName;

    private String position;

    private LocalDate entryDate;

    /** 试用期月数，用于自动生成试用期区间 */
    private Integer probationMonths;

    /** 招聘录用 / 内推 / 校招 / 其他 */
    private String source;

    /** 0待关联档案 1已关联档案 */
    private Integer status;

    private String remark;

    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
