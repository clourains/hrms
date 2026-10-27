package com.hrms.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 候选人录用入参
 *
 * 录用不是简单改个状态：要真给员工表插一条记录，并生成试用期跟踪。
 * 岗位、部门默认取招聘需求上的值，允许 HR 在录用时改（实际到岗部门和岗位可能不同）。
 */
@Data
public class HireDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 入职日期，不传默认今天 */
    private LocalDate entryDate;

    /** 入职部门，不传取招聘需求上的部门 */
    private Long deptId;

    /** 入职岗位，不传取招聘需求上的岗位 */
    private String position;

    /** 职级，如 P5 */
    private String jobLevel;

    /** 月薪（K），不传取候选人期望薪资 */
    private BigDecimal salary;

    /** 试用期月数，不传默认 3 个月 */
    private Integer probationMonths;

    /** 带教导师 */
    private String mentor;

    /** 工作地点 */
    private String workLocation;
}
