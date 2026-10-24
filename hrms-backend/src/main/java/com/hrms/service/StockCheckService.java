package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockCheckOrderDTO;

import java.util.Map;

/**
 * 库存盘点单Service（P019）
 *
 * 与 InventoryService#checkStock 的区别：
 * - checkStock 是行内快捷盘点，单 SKU、填完立即改账、无单据
 * - 这里的盘点单支持多 SKU、草稿可改、确认时才批量改账，并留单号可追溯
 */
public interface StockCheckService {

    /**
     * 按仓库带出当前库存行，供建单时勾选
     */
    Map<String, Object> inventoryRows(Long warehouseId);

    /**
     * 新建盘点单（草稿），返回单ID
     */
    Long create(StockCheckOrderDTO dto);

    /**
     * 修改草稿（整单覆盖明细）
     */
    void update(StockCheckOrderDTO dto);

    /**
     * 确认盘点：按差异批量调整库存并写流水，同一事务
     */
    Map<String, Object> confirm(Long id);

    /**
     * 作废草稿
     */
    void cancel(Long id);

    /**
     * 盘点单分页
     */
    PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO);

    /**
     * 盘点单详情（单头 + 明细）
     */
    Map<String, Object> detail(Long id);
}
