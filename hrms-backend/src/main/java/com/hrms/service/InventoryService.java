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
     * 分页查询库存变动记录（含产品名、SKU、仓库名、操作人）
     */
    PageResult<Map<String, Object>> logPageQuery(BizQueryDTO queryDTO);

    /**
     * 按产品+仓库查询库存
     */
    Inventory getByProductAndWarehouse(Long productId, Long warehouseId);

    /**
     * 低库存预警列表
     */
    List<Map<String, Object>> warningList();

    /**
     * 修改库存预警值（P035）
     */
    boolean updateWarningQuantity(Long id, Integer warningQuantity);

    /**
     * 仓间调拨：从 A 仓出库、B 仓入库，同一事务（P019）
     */
    boolean transfer(Long productId, Long fromWarehouseId, Long toWarehouseId, Integer quantity, String remark);

    /**
     * 盘点：把库存调整成实盘数量，差异写流水（P019）
     */
    Map<String, Object> checkStock(Long productId, Long warehouseId, Integer actualQuantity, String remark);

    /**
     * 预警数量统计，供顶部通知红点使用（P022）
     */
    int warningCount();
}
