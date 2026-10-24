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
     * 变动类型（库存流水专用）
     * 前端提交的是 changeType，历史代码读的是 type，两个都收，避免筛选条件被静默丢弃（P007）
     */
    private Integer changeType;

    /**
     * 只看低库存（库存总览专用，P008）
     */
    private Boolean lowStock;

    /**
     * 排序字段（前端传实体属性名，后端白名单换算）
     */
    private String sortField;

    /**
     * 排序方向 asc / desc
     */
    private String sortOrder;

    /**
     * 是否包含已下架产品（产品下拉专用，P021）
     */
    private Boolean includeDisabled;

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

    /**
     * 页码下限保护：小于 1 一律当第 1 页
     */
    public Integer getPageNum() {
        return (pageNum == null || pageNum < 1) ? 1 : pageNum;
    }

    /**
     * 每页条数上限保护：最大 100 条，防止传大数一次拉全库（P030）
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
