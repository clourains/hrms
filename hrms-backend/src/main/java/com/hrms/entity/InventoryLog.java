package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存变动记录实体类
 */
@Data
@TableName("biz_inventory_log")
public class InventoryLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long warehouseId;

    private Integer changeType;

    private Integer quantity;

    private Integer beforeQuantity;

    /**
     * 变动前锁定数量（P020）
     */
    private Integer beforeLockedQuantity;

    private Integer afterQuantity;

    /**
     * 变动后锁定数量（P020）
     */
    private Integer afterLockedQuantity;

    private String orderNo;

    private String operator;

    private String remark;

    private LocalDateTime createTime;
}
