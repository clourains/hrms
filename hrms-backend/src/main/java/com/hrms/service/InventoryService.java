package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockChangeDTO;
import com.hrms.entity.Inventory;
import com.hrms.entity.InventoryLog;

import java.util.List;
import java.util.Map;

/**
 * 库存Service接口
 */
public interface InventoryService {

    /**
     * 分页查询库存（含产品名、仓库名）
     */
    PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO);

    /**
     * 库存变动（入库/出库/锁定/解锁）
     */
    boolean changeStock(StockChangeDTO dto);

    /**
     * 分页查询库存变动记录
     */
    PageResult<InventoryLog> logPageQuery(BizQueryDTO queryDTO);

    /**
     * 按产品+仓库查询库存
     */
    Inventory getByProductAndWarehouse(Long productId, Long warehouseId);

    /**
     * 低库存预警列表
     */
    List<Map<String, Object>> warningList();
}
