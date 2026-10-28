package com.hrms.dto;

import lombok.Data;

/**
 * 入离管理（离职/辞退）查询DTO
 *
 * 分页与关键字沿用 BizQueryDTO，额外补类别/类型/部门与预计生效日期区间。
 */
@Data
public class EmployeeExitQueryDTO extends BizQueryDTO {

    private static final long serialVersionUID = 1L;

    /** 1离职 2辞退 */
    private Integer exitKind;

    /** 1主动离职 2合同到期 3退休 4辞退 5裁员 6开除 */
    private Integer exitType;

    private Long deptId;

    /** 预计生效日期起（含） */
    private java.time.LocalDate startDate;

    /** 预计生效日期止（含） */
    private java.time.LocalDate endDate;

    /** 只看待审批 */
    private Boolean pendingOnly;
}
