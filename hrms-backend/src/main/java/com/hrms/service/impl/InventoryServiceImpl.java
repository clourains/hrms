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
import com.hrms.util.SortUtil;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存Service实现类
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 与 biz_inventory_log.remark 的实际长度保持一致（P029） */
    private static final int MAX_REMARK_LENGTH = 200;
    private static final int MAX_OPERATOR_LENGTH = 50;
    private static final int MAX_ORDER_NO_LENGTH = 50;

    /** 没有历史阈值时的兜底预警值，沿用原来的业务默认 */
    private static final int DEFAULT_WARNING_QUANTITY = 100;

    /** 库存总览可排序字段白名单（P004） */
    private static final Map<String, SortUtil.OrderCol> INVENTORY_SORT = new HashMap<>();
    /** 变动记录可排序字段白名单 */
    private static final Map<String, SortUtil.OrderCol> LOG_SORT = new HashMap<>();

    static {
        INVENTORY_SORT.put("quantity", SortUtil.OrderCol.plain("quantity"));
        INVENTORY_SORT.put("lockedQuantity", SortUtil.OrderCol.plain("locked_quantity"));
        INVENTORY_SORT.put("availableQuantity",
                SortUtil.OrderCol.plain("(IFNULL(quantity,0) - IFNULL(locked_quantity,0))"));
        INVENTORY_SORT.put("warningQuantity", SortUtil.OrderCol.plain("warning_quantity"));
        INVENTORY_SORT.put("updateTime", SortUtil.OrderCol.plain("update_time"));
        // SKU 是"字母+数字"，按自然序排，SKU-2 在 SKU-10 前面
        INVENTORY_SORT.put("sku",
                SortUtil.OrderCol.natural("(SELECT p.sku FROM biz_product p WHERE p.id = biz_inventory.product_id)"));

        LOG_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
        LOG_SORT.put("changeType", SortUtil.OrderCol.plain("change_type"));
        LOG_SORT.put("quantity", SortUtil.OrderCol.plain("quantity"));
    }

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
        // 只看低库存：可用量 <= 预警值，与前端标红口径保持一致（P008 / P010）
        if (Boolean.TRUE.equals(queryDTO.getLowStock())) {
            wrapper.apply("(IFNULL(quantity,0) - IFNULL(locked_quantity,0)) <= IFNULL(warning_quantity,0)");
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), INVENTORY_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(Inventory::getUpdateTime);
        }

        Page<Inventory> result = inventoryMapper.selectPage(page, wrapper);
        List<Map<String, Object>> list = enrichInventories(result.getRecords());
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStock(StockChangeDTO dto) {
        validateStockChange(dto);

        // 加行锁，避免两个人同时出库同一行造成超卖（P013）
        Inventory inventory = inventoryMapper.selectForUpdate(dto.getProductId(), dto.getWarehouseId());
        if (inventory == null) {
            return createInventoryAndChange(dto);
        }
        return applyChange(inventory, dto);
    }

    /**
     * 首次出现"产品+仓库"组合时创建库存行。
     * 并发下可能两个事务同时插入，唯一键会挡住后一个，此时重新加锁读取再改。
     */
    private boolean createInventoryAndChange(StockChangeDTO dto) {
        Inventory fresh = new Inventory();
        fresh.setProductId(dto.getProductId());
        fresh.setWarehouseId(dto.getWarehouseId());
        fresh.setQuantity(0);
        fresh.setLockedQuantity(0);
        // 同一产品在别的仓库已有阈值时继承，避免每个仓库预警线不一样（P033）
        fresh.setWarningQuantity(resolveWarningQuantity(dto.getProductId()));
        fresh.setUpdateTime(LocalDateTime.now());

        try {
            inventoryMapper.insert(fresh);
        } catch (DuplicateKeyException e) {
            Inventory locked = inventoryMapper.selectForUpdate(dto.getProductId(), dto.getWarehouseId());
            if (locked == null) {
                throw new RuntimeException("库存记录创建失败，请重试");
            }
            return applyChange(locked, dto);
        }
        return applyChange(fresh, dto);
    }

    /**
     * 在同一事务里改库存并写流水
     */
    private boolean applyChange(Inventory inventory, StockChangeDTO dto) {
        int quantity = dto.getQuantity();
        int before = nvl(inventory.getQuantity());
        int beforeLocked = nvl(inventory.getLockedQuantity());
        int after = before;
        int afterLocked = beforeLocked;
        int changeType = dto.getChangeType();

        switch (changeType) {
            case 1: // 入库
                after = before + quantity;
                break;
            case 2: // 出库
                int available = before - beforeLocked;
                if (available < quantity) {
                    throw new RuntimeException("可用库存不足，当前可用 " + available + "，无法出库 " + quantity);
                }
                // 出库优先消耗已锁定的数量，否则锁定量会一直挂着、可用量被永久扣掉（P036）
                int fromLocked = Math.min(beforeLocked, quantity);
                afterLocked = beforeLocked - fromLocked;
                after = before - quantity;
                break;
            case 3: // 锁定
                if (before - beforeLocked < quantity) {
                    throw new RuntimeException("可用库存不足，当前可用 " + (before - beforeLocked) + "，无法锁定 " + quantity);
                }
                afterLocked = beforeLocked + quantity;
                break;
            case 4: // 解锁
                if (beforeLocked < quantity) {
                    throw new RuntimeException("锁定库存不足，当前锁定 " + beforeLocked + "，无法解锁 " + quantity);
                }
                afterLocked = beforeLocked - quantity;
                break;
            default:
                throw new RuntimeException("不支持的变动类型");
        }

        inventory.setQuantity(after);
        inventory.setLockedQuantity(afterLocked);
        inventory.setUpdateTime(LocalDateTime.now());
        if (inventory.getId() == null) {
            inventoryMapper.insert(inventory);
        } else {
            inventoryMapper.updateById(inventory);
        }

        writeLog(dto, before, beforeLocked, after, afterLocked);
        return true;
    }

    /**
     * 写变动记录，操作人没传时从登录态补（P009）
     */
    private void writeLog(StockChangeDTO dto, int before, int beforeLocked, int after, int afterLocked) {
        InventoryLog log = new InventoryLog();
        log.setProductId(dto.getProductId());
        log.setWarehouseId(dto.getWarehouseId());
        log.setChangeType(dto.getChangeType());
        log.setQuantity(dto.getQuantity());
        log.setBeforeQuantity(before);
        log.setBeforeLockedQuantity(beforeLocked);
        log.setAfterQuantity(after);
        log.setAfterLockedQuantity(afterLocked);
        log.setOrderNo(trimToNull(dto.getOrderNo(), MAX_ORDER_NO_LENGTH));
        log.setOperator(resolveOperator(dto.getOperator()));
        log.setRemark(trimToNull(dto.getRemark(), MAX_REMARK_LENGTH));
        log.setCreateTime(LocalDateTime.now());
        inventoryLogMapper.insert(log);
    }

    private String resolveOperator(String operator) {
        if (operator != null && !operator.trim().isEmpty()) {
            return trimToLength(operator, MAX_OPERATOR_LENGTH);
        }
        // 系统自动触发的流水（下单锁定、发货出库）传的是"系统"，这里只兜底人工操作
        return trimToLength(UserContext.getOrDefault("系统"), MAX_OPERATOR_LENGTH);
    }

    private void validateStockChange(StockChangeDTO dto) {
        if (dto == null || dto.getProductId() == null || dto.getWarehouseId() == null
                || dto.getQuantity() == null || dto.getQuantity() <= 0
                || dto.getChangeType() == null) {
            throw new RuntimeException("参数不完整");
        }
        if (dto.getChangeType() < 1 || dto.getChangeType() > 4) {
            throw new RuntimeException("不支持的变动类型");
        }
        if (dto.getRemark() != null && dto.getRemark().length() > MAX_REMARK_LENGTH) {
            throw new RuntimeException("备注最多 " + MAX_REMARK_LENGTH + " 个字");
        }
    }

    @Override
    public PageResult<Map<String, Object>> logPageQuery(BizQueryDTO queryDTO) {
        Page<InventoryLog> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<InventoryLog> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getProductId() != null) {
            wrapper.eq(InventoryLog::getProductId, queryDTO.getProductId());
        }
        if (queryDTO.getWarehouseId() != null) {
            wrapper.eq(InventoryLog::getWarehouseId, queryDTO.getWarehouseId());
        }
        // 前端提交的是 changeType，先读它，再向后兼容 type（P007）
        Integer changeType = queryDTO.getChangeType() != null ? queryDTO.getChangeType() : queryDTO.getType();
        if (changeType != null) {
            wrapper.eq(InventoryLog::getChangeType, changeType);
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(InventoryLog::getCreateTime, queryDTO.getStartDate().atStartOfDay());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.lt(InventoryLog::getCreateTime, queryDTO.getEndDate().plusDays(1).atStartOfDay());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), LOG_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(InventoryLog::getCreateTime).orderByDesc(InventoryLog::getId);
        }

        Page<InventoryLog> result = inventoryLogMapper.selectPage(page, wrapper);
        List<Map<String, Object>> list = enrichLogs(result.getRecords());
        return new PageResult<>(result.getTotal(), list);
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
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        // 与前端、与"只看低库存"筛选统一为 <=（P010）
        wrapper.apply("(IFNULL(quantity,0) - IFNULL(locked_quantity,0)) <= IFNULL(warning_quantity,0)");
        List<Inventory> all = inventoryMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> map : enrichInventories(all)) {
            result.add(map);
        }
        return result;
    }

    @Override
    public int warningCount() {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("(IFNULL(quantity,0) - IFNULL(locked_quantity,0)) <= IFNULL(warning_quantity,0)");
        return Math.toIntExact(inventoryMapper.selectCount(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWarningQuantity(Long id, Integer warningQuantity) {
        if (id == null) {
            throw new RuntimeException("库存记录不存在");
        }
        if (warningQuantity == null || warningQuantity < 0) {
            throw new RuntimeException("预警值不能为负数");
        }
        Inventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) {
            throw new RuntimeException("库存记录不存在");
        }
        inventory.setWarningQuantity(warningQuantity);
        inventory.setUpdateTime(LocalDateTime.now());
        return inventoryMapper.updateById(inventory) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transfer(Long productId, Long fromWarehouseId, Long toWarehouseId, Integer quantity, String remark) {
        if (productId == null || fromWarehouseId == null || toWarehouseId == null
                || quantity == null || quantity <= 0) {
            throw new RuntimeException("调拨参数不完整");
        }
        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new RuntimeException("调出仓库和调入仓库不能相同");
        }
        Warehouse from = warehouseMapper.selectById(fromWarehouseId);
        Warehouse to = warehouseMapper.selectById(toWarehouseId);
        if (from == null || to == null) {
            throw new RuntimeException("仓库不存在或已删除");
        }

        String suffix = (remark == null || remark.trim().isEmpty()) ? "" : "（" + remark.trim() + "）";

        StockChangeDTO out = new StockChangeDTO();
        out.setProductId(productId);
        out.setWarehouseId(fromWarehouseId);
        out.setChangeType(2);
        out.setQuantity(quantity);
        out.setRemark(trimToLength("调拨出库 → " + to.getWarehouseName() + suffix, MAX_REMARK_LENGTH));
        changeStock(out);

        StockChangeDTO in = new StockChangeDTO();
        in.setProductId(productId);
        in.setWarehouseId(toWarehouseId);
        in.setChangeType(1);
        in.setQuantity(quantity);
        in.setRemark(trimToLength("调拨入库 ← " + from.getWarehouseName() + suffix, MAX_REMARK_LENGTH));
        changeStock(in);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkStock(Long productId, Long warehouseId, Integer actualQuantity, String remark) {
        if (productId == null || warehouseId == null || actualQuantity == null || actualQuantity < 0) {
            throw new RuntimeException("盘点参数不完整");
        }

        Inventory inventory = inventoryMapper.selectForUpdate(productId, warehouseId);
        if (inventory == null) {
            if (actualQuantity == 0) {
                throw new RuntimeException("该产品在该仓库还没有库存记录，实盘为 0 时无需盘点");
            }
            inventory = new Inventory();
            inventory.setProductId(productId);
            inventory.setWarehouseId(warehouseId);
            inventory.setQuantity(0);
            inventory.setLockedQuantity(0);
            inventory.setWarningQuantity(resolveWarningQuantity(productId));
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryMapper.insert(inventory);
        }

        int before = nvl(inventory.getQuantity());
        int locked = nvl(inventory.getLockedQuantity());
        int diff = actualQuantity - before;
        if (diff != 0) {
            inventory.setQuantity(actualQuantity);
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryMapper.updateById(inventory);

            // 盘点直接校准总量，不触碰锁定量，因此单独写流水而不再走 changeStock
            InventoryLog log = new InventoryLog();
            log.setProductId(productId);
            log.setWarehouseId(warehouseId);
            log.setChangeType(diff > 0 ? 1 : 2);
            log.setQuantity(Math.abs(diff));
            log.setBeforeQuantity(before);
            log.setBeforeLockedQuantity(locked);
            log.setAfterQuantity(actualQuantity);
            log.setAfterLockedQuantity(locked);
            log.setOperator(trimToLength(UserContext.getOrDefault("系统"), MAX_OPERATOR_LENGTH));
            log.setRemark(trimToLength("盘点调整：" + before + " → " + actualQuantity
                    + (remark == null || remark.trim().isEmpty() ? "" : "，" + remark.trim()), MAX_REMARK_LENGTH));
            log.setCreateTime(LocalDateTime.now());
            inventoryLogMapper.insert(log);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("beforeQuantity", before);
        result.put("actualQuantity", actualQuantity);
        result.put("diff", diff);
        result.put("lockedQuantity", locked);
        return result;
    }

    /**
     * 批量装配库存行，避免每行单查产品/仓库（P017）
     */
    private List<Map<String, Object>> enrichInventories(List<Inventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> productIds = inventories.stream()
                .map(Inventory::getProductId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> warehouseIds = inventories.stream()
                .map(Inventory::getWarehouseId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Collections.emptyMap()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w, (a, b) -> a));

        List<Map<String, Object>> list = new ArrayList<>(inventories.size());
        for (Inventory inv : inventories) {
            list.add(enrichInventory(inv, productMap.get(inv.getProductId()), warehouseMap.get(inv.getWarehouseId())));
        }
        return list;
    }

    /**
     * 批量装配变动记录（P017）
     */
    private List<Map<String, Object>> enrichLogs(List<InventoryLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> productIds = logs.stream()
                .map(InventoryLog::getProductId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> warehouseIds = logs.stream()
                .map(InventoryLog::getWarehouseId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Collections.emptyMap()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w, (a, b) -> a));

        List<Map<String, Object>> list = new ArrayList<>(logs.size());
        for (InventoryLog log : logs) {
            list.add(enrichLog(log, productMap.get(log.getProductId()), warehouseMap.get(log.getWarehouseId())));
        }
        return list;
    }

    /**
     * 把变动记录补成前端直接能渲染的结构：
     * 加上产品名/SKU/仓库名/操作人，并把 quantity 同时以 changeQuantity 暴露，
     * 字段名与 WarehouseList.vue 的表格列保持一致。
     */
    private Map<String, Object> enrichLog(InventoryLog log, Product product, Warehouse warehouse) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("productId", log.getProductId());
        map.put("warehouseId", log.getWarehouseId());
        map.put("changeType", log.getChangeType());
        map.put("quantity", log.getQuantity());
        map.put("changeQuantity", log.getQuantity());
        map.put("beforeQuantity", log.getBeforeQuantity());
        map.put("beforeLockedQuantity", log.getBeforeLockedQuantity());
        map.put("afterQuantity", log.getAfterQuantity());
        map.put("afterLockedQuantity", log.getAfterLockedQuantity());
        map.put("orderNo", log.getOrderNo());
        map.put("operator", log.getOperator());
        map.put("operatorName", log.getOperator());
        map.put("remark", log.getRemark());
        if (log.getCreateTime() != null) {
            map.put("createTime", log.getCreateTime().format(LOG_TIME_FORMATTER));
        }

        if (product != null) {
            map.put("productName", product.getName());
            map.put("productNameEn", product.getNameEn());
            map.put("sku", product.getSku());
            map.put("unit", product.getUnit());
        } else {
            // 产品被删掉后不要再显示空白（P050）
            map.put("productName", "（产品已删除）");
            map.put("sku", "-");
        }
        if (warehouse != null) {
            map.put("warehouseName", warehouse.getWarehouseName());
            map.put("warehouseType", warehouse.getType());
        } else {
            // 仓库被删掉后不要再显示空白（P014）
            map.put("warehouseName", "（仓库已删除）");
        }
        return map;
    }

    private Map<String, Object> enrichInventory(Inventory inv, Product product, Warehouse warehouse) {
        int quantity = nvl(inv.getQuantity());
        int locked = nvl(inv.getLockedQuantity());

        Map<String, Object> map = new HashMap<>();
        map.put("id", inv.getId());
        map.put("productId", inv.getProductId());
        map.put("warehouseId", inv.getWarehouseId());
        map.put("quantity", quantity);
        map.put("lockedQuantity", locked);
        map.put("availableQuantity", quantity - locked);
        map.put("warningQuantity", inv.getWarningQuantity());
        map.put("updateTime", inv.getUpdateTime());
        map.put("lowStock", (quantity - locked) <= nvl(inv.getWarningQuantity()));

        if (product != null) {
            map.put("productName", product.getName());
            map.put("productNameEn", product.getNameEn());
            map.put("sku", product.getSku());
            map.put("unit", product.getUnit());
            map.put("salePriceUsd", product.getSalePriceUsd());
            map.put("costPrice", product.getCostPrice());
        } else {
            // 产品被删掉后不要再显示空白（P050）
            map.put("productName", "（产品已删除）");
            map.put("sku", "-");
        }
        if (warehouse != null) {
            map.put("warehouseName", warehouse.getWarehouseName());
            map.put("warehouseType", warehouse.getType());
            map.put("country", warehouse.getCountry());
        } else {
            map.put("warehouseName", "（仓库已删除）");
        }
        return map;
    }

    /**
     * 同产品其它仓库已有的预警值优先继承（P033）
     */
    private int resolveWarningQuantity(Long productId) {
        if (productId == null) {
            return DEFAULT_WARNING_QUANTITY;
        }
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getProductId, productId)
                .isNotNull(Inventory::getWarningQuantity)
                .orderByAsc(Inventory::getId)
                .last("LIMIT 1");
        Inventory exist = inventoryMapper.selectOne(wrapper);
        if (exist != null && exist.getWarningQuantity() != null) {
            return exist.getWarningQuantity();
        }
        return DEFAULT_WARNING_QUANTITY;
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private static String trimToNull(String value, int maxLength) {
        return trimToLength(value, maxLength);
    }
}
