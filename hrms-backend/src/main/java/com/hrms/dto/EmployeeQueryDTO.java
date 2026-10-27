package com.hrms.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 员工查询DTO
 */
@Data
public class EmployeeQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 姓名
     */
    private String name;

    /**
     * 工号（支持模糊匹配）
     * 补这个字段的原因：员工列表页一直有「工号」搜索框，但前端没往请求里带、
     * 后端也没有这个字段，输入工号点搜索会原样返回全量，看起来像筛选成功其实是没筛。
     */
    private String empNo;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 职位
     */
    private String position;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
