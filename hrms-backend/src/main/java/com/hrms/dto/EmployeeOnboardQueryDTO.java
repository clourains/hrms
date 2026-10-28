package com.hrms.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 入职登记查询DTO
 */
@Data
public class EmployeeOnboardQueryDTO extends BizQueryDTO {

    private static final long serialVersionUID = 1L;

    private Long deptId;

    /** 0待关联档案 1已关联档案 */
    private Integer status;

    /** 入职日期起（含） */
    private LocalDate startDate;

    /** 入职日期止（含） */
    private LocalDate endDate;
}
