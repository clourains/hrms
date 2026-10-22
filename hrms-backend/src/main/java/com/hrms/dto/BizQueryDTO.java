package com.hrms.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 贸易业务通用查询DTO
 */
@Data
public class BizQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关键字（名称/编码模糊匹配）
     */
    private String keyword;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 类型（仓库类型/变动类型/收付类型/运输方式）
     */
    private Integer type;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 国家
     */
    private String country;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
