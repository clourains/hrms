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
 * 员工实体类
 */
@Data
@TableName("sys_employee")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String empNo;

    private String name;

    private Integer gender;

    private LocalDate birthday;

    private String idCard;

    private String phone;

    private String email;

    private String address;

    private String avatar;

    private Long deptId;

    private String position;

    private String jobLevel;

    private LocalDate entryDate;

    private Integer workYears;

    private String education;

    private String major;

    private BigDecimal salary;

    private Integer status;

    /**
     * 离职日期（P045 之前的 M1-5）：办理离职时填写，用于入离职趋势真实统计
     */
    private LocalDate leaveDate;

    private String country;

    private String workLocation;

    private Integer employeeType;

    private String passportNo;

    private String visaType;

    private LocalDate visaExpireDate;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
