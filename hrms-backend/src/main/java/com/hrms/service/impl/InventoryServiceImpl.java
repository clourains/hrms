package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockChangeDTO;
import com.hrms.entity.Inventory;
import com.hrms.entity.InventoryLog;
import com.hrms.entity.Product;
import com.hrms.entity.Warehouse;
import com.hrms.mapper.InventoryLogMapper;
import com.hrms.mapper.InventoryMapper;
import com.hrms.mapper.ProductMapper;
import com.hrms.mapper.WarehouseMapper;
import com.hrms.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存Service实现类
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO) {
        Page<Inventory> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getProductId() != null) {
            wrapper.eq(Inventory::getProductId, queryDTO.getProductId());
        }
        if (queryDTO.getWarehouseId() != null) {
            wrapper.eq(Inventory::getWarehouseId, queryDTO.getWarehouseId());
        }
        wrapper.orderByDesc(Inventory::getUpdateTime);
        Page<Inventory> result = inventoryMapper.selectPage(page, wrapper);

        List<Map<String, Object>> list = result.getRecords().stream()
                .map(this::enrichInventory)
                .collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStock(StockChangeDTO dto) {
        if (dto.getProductId() == null || dto.getWarehouseId() == null
                || dto.getQuantity() == null || dto.getQuantity() <= 0
                || dto.getChangeType() == null) {
            throw new RuntimeException("参数不完整");
        }

        Inventory inventory = getByProductAndWarehouse(dto.getProductId(), dto.getWarehouseId());
        int before = inventory == null ? 0 : (inventory.getQuantity() == null ? 0 : inventory.getQuantity());
        int beforeLocked = inventory == null ? 0 : (inventory.getLockedQuantity() == null ? 0 : inventory.getLockedQuantity());
        int after = before;
        int afterLocked = beforeLocked;

        switch (dto.getChangeType()) {
            case 1: // 入库
                after = before + dto.getQuantity();
                break;
            case 2: // 出库
                if (before - beforeLocked < dto.getQuantity()) {
                    throw new RuntimeException("可用库存不足，无法出库");
                }
                after = before - dto.getQuantity();
                break;
            case 3: // 锁定
                if (before - beforeLocked < dto.getQuantity()) {
                    throw new RuntimeException("可用库存不足，无法锁定");
                }
                afterLocked = beforeLocked + dto.getQuantity();
                break;
            case 4: // 解锁
                if (beforeLocked < dto.getQuantity()) {
                    throw new RuntimeException("锁定库存不足，无法解锁");
                }
                afterLocked = beforeLocked - dto.getQuantity();
                break;
            default:
                throw new RuntimeException("不支持的变动类型");
        }

        if (inventory == null) {
            inventory = new Inventory();
            inventory.setProductId(dto.getProductId());
            inventory.setWarehouseId(dto.getWarehouseId());
            inventory.setQuantity(after);
            inventory.setLockedQuantity(afterLocked);
            inventory.setWarningQuantity(100);
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryMapper.insert(inventory);
        } else {
            inventory.setQuantity(after);
            inventory.setLockedQuantity(afterLocked);
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryMapper.updateById(inventory);
        }

        // 写变动记录
        InventoryLog log = new InventoryLog();
        log.setProductId(dto.getProductId());
        log.setWarehouseId(dto.getWarehouseId());
        log.setChangeType(dto.getChangeType());
        log.setQuantity(dto.getQuantity());
        log.setBeforeQuantity(before);
        log.setAfterQuantity(after);
        log.setOrderNo(dto.getOrderNo());
        log.setOperator(dto.getOperator());
        log.setRemark(dto.getRemark());
        log.setCreateTime(LocalDateTime.now());
        inventoryLogMapper.insert(log);

        return true;
    }

    @Override
    public PageResult<InventoryLog> logPageQuery(BizQueryDTO queryDTO) {
        Page<InventoryLog> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<InventoryLog> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getProductId() != null) {
            wrapper.eq(InventoryLog::getProductId, queryDTO.getProductId());
        }
        if (queryDTO.getWarehouseId() != null) {
            wrapper.eq(InventoryLog::getWarehouseId, queryDTO.getWarehouseId());
        }
        if (queryDTO.getType() != null) {
            wrapper.eq(InventoryLog::getChangeType, queryDTO.getType());
        }
        wrapper.orderByDesc(InventoryLog::getCreateTime);
        Page<InventoryLog> result = inventoryLogMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Inventory getByProductAndWarehouse(Long productId, Long warehouseId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getProductId, productId)
                .eq(Inventory::getWarehouseId, warehouseId);
        return inventoryMapper.selectOne(wrapper);
    }

    @Override
    public List<Map<String, Object>> warningList() {
        List<Inventory> all = inventoryMapper.selectList(new LambdaQueryWrapper<>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Inventory inv : all) {
            int available = (inv.getQuantity() == null ? 0 : inv.getQuantity())
                    - (inv.getLockedQuantity() == null ? 0 : inv.getLockedQuantity());
            int warning = inv.getWarningQuantity() == null ? 100 : inv.getWarningQuantity();
            if (available < warning) {
                Map<String, Object> map = enrichInventory(inv);
                map.put("availableQuantity", available);
                result.add(map);
            }
        }
        return result;
    }

    private Map<String, Object> enrichInventory(Inventory inv) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", inv.getId());
        map.put("productId", inv.getProductId());
        map.put("warehouseId", inv.getWarehouseId());
        map.put("quantity", inv.getQuantity());
        map.put("lockedQuantity", inv.getLockedQuantity());
        map.put("availableQuantity", (inv.getQuantity() == null ? 0 : inv.getQuantity())
                - (inv.getLockedQuantity() == null ? 0 : inv.getLockedQuantity()));
        map.put("warningQuantity", inv.getWarningQuantity());
        map.put("updateTime", inv.getUpdateTime());

        Product product = productMapper.selectById(inv.getProductId());
        if (product != null) {
            map.put("productName", product.getName());
            map.put("productNameEn", product.getNameEn());
            map.put("sku", product.getSku());
            map.put("unit", product.getUnit());
            map.put("salePriceUsd", product.getSalePriceUsd());
            map.put("costPrice", product.getCostPrice());
        }
        Warehouse warehouse = warehouseMapper.selectById(inv.getWarehouseId());
        if (warehouse != null) {
            map.put("warehouseName", warehouse.getWarehouseName());
            map.put("warehouseType", warehouse.getType());
            map.put("country", warehouse.getCountry());
        }
        return map;
    }
}
