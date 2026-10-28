package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工离职/辞退单据（入离管理）
 *
 * 一张表承载两类流程：exitKind=1 离职、exitKind=2 辞退。
 * 员工姓名/工号/部门/岗位做冗余快照，员工档案后续被修改也不影响单据还原。
 */
@Data
@TableName("hr_employee_exit")
public class EmployeeExit implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 类别：离职 */
    public static final int KIND_RESIGN = 1;
    /** 类别：辞退 */
    public static final int KIND_DISMISS = 2;

    /** 状态：待审批 */
    public static final int STATUS_PENDING = 0;
    /** 状态：已批准 */
    public static final int STATUS_APPROVED = 1;
    /** 状态：已驳回 */
    public static final int STATUS_REJECTED = 2;
    /** 状态：已生效 */
    public static final int STATUS_EFFECTIVE = 3;
    /** 状态：已撤销 */
    public static final int STATUS_CANCELED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单据编号 LZ/CT + 时间戳 */
    private String exitNo;

    /** 1离职 2辞退 */
    private Integer exitKind;

    /** 1主动离职 2合同到期 3退休 4辞退 5裁员 6开除 */
    private Integer exitType;

    private Long employeeId;

    private String empNo;

    private String employeeName;

    private Long deptId;

    private String deptName;

    private String position;

    /** 发起日期 */
    private LocalDate applyDate;

    /** 预计离职/辞退生效日期 */
    private LocalDate expectLeaveDate;

    /** 实际生效日期（点「生效」时写入） */
    private LocalDate actualLeaveDate;

    private String reason;

    /** 工作交接人 */
    private String handoverTo;

    /** 补偿金额（辞退/裁员适用） */
    private BigDecimal compensation;

    /** 0待审批 1已批准 2已驳回 3已生效 4已撤销 */
    private Integer status;

    private String approveBy;

    private LocalDateTime approveTime;

    private String approveRemark;

    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
