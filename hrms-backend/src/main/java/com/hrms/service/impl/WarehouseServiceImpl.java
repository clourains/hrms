package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Inventory;
import com.hrms.entity.InventoryLog;
import com.hrms.entity.OrderItem;
import com.hrms.entity.StockCheck;
import com.hrms.entity.Warehouse;
import com.hrms.mapper.InventoryLogMapper;
import com.hrms.mapper.InventoryMapper;
import com.hrms.mapper.OrderItemMapper;
import com.hrms.mapper.StockCheckMapper;
import com.hrms.mapper.WarehouseMapper;
import com.hrms.service.WarehouseService;
import com.hrms.util.CodeReleaser;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仓库Service实现类
 */
@Service
public class WarehouseServiceImpl implements WarehouseService {

    /** 仓库列表可排序字段白名单（P004） */
    private static final Map<String, SortUtil.OrderCol> WAREHOUSE_SORT = new HashMap<>();

    static {
        WAREHOUSE_SORT.put("warehouseCode", SortUtil.OrderCol.natural("warehouse_code"));
        WAREHOUSE_SORT.put("warehouseName", SortUtil.OrderCol.natural("warehouse_name"));
        WAREHOUSE_SORT.put("country", SortUtil.OrderCol.natural("country"));
        WAREHOUSE_SORT.put("city", SortUtil.OrderCol.natural("city"));
        WAREHOUSE_SORT.put("capacity", SortUtil.OrderCol.plain("capacity"));
        WAREHOUSE_SORT.put("status", SortUtil.OrderCol.plain("status"));
        WAREHOUSE_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private StockCheckMapper stockCheckMapper;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Override
    public PageResult<Warehouse> pageQuery(BizQueryDTO queryDTO) {
        Page<Warehouse> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Warehouse::getWarehouseName, queryDTO.getKeyword())
                    .or().like(Warehouse::getWarehouseCode, queryDTO.getKeyword())
                    .or().like(Warehouse::getCity, queryDTO.getKeyword())
                    .or().like(Warehouse::getCountry, queryDTO.getKeyword()));
        }
        if (queryDTO.getType() != null) {
            wrapper.eq(Warehouse::getType, queryDTO.getType());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Warehouse::getStatus, queryDTO.getStatus());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), WAREHOUSE_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByAsc(Warehouse::getWarehouseCode);
        }
        Page<Warehouse> result = warehouseMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Warehouse getById(Long id) {
        return warehouseMapper.selectById(id);
    }

    @Override
    public boolean save(Warehouse warehouse) {
        Validators.text(warehouse.getWarehouseCode(), "仓库编码");
        Validators.text(warehouse.getWarehouseName(), "仓库名称");
        Validators.notNull(warehouse.getType(), "仓库类型");
        Validators.text(warehouse.getCountry(), "国家");
        Validators.text(warehouse.getCity(), "城市");
        Validators.text(warehouse.getAddress(), "详细地址");
        Validators.text(warehouse.getManager(), "负责人");
        Validators.nonNegative(warehouse.getCapacity(), "容量");
        if (warehouse.getStatus() == null) {
            warehouse.setStatus(1);
        }
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseMapper.insert(warehouse) > 0;
    }

    @Override
    public boolean update(Warehouse warehouse) {
        if (warehouse.getId() == null) {
            throw new RuntimeException("仓库不存在");
        }
        Warehouse origin = warehouseMapper.selectById(warehouse.getId());
        if (origin == null) {
            throw new RuntimeException("仓库不存在或已删除");
        }
        Validators.notCleared(origin.getWarehouseCode(), warehouse.getWarehouseCode(), "仓库编码");
        Validators.notCleared(origin.getWarehouseName(), warehouse.getWarehouseName(), "仓库名称");
        Validators.notCleared(origin.getCountry(), warehouse.getCountry(), "国家");
        Validators.notCleared(origin.getCity(), warehouse.getCity(), "城市");
        Validators.notCleared(origin.getAddress(), warehouse.getAddress(), "详细地址");
        Validators.notCleared(origin.getManager(), warehouse.getManager(), "负责人");
        Validators.nonNegative(warehouse.getCapacity(), "容量");
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseMapper.updateById(warehouse) > 0;
    }

    /**
     * 删除仓库（P014、P043、P050）
     *
     * P014：以前只把仓库逻辑删除，biz_inventory 里它的库存行还留着，查库存时仓库名变成空白。
     * P050：仓库被订单明细、盘点单、出入库流水引用时同样不能删——这些地方都是拿 warehouse_id
     * 现查仓库名的，@TableLogic 一过滤就只剩 `#ID` 或空白。停用的仓库应该改状态，不是删除。
     * P043：逻辑删除前把编码让出来，否则 uk_warehouse_code 会被已删记录一直占着。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在或已删除");
        }

        List<String> refs = new ArrayList<>();

        Long orderItemCount = orderItemMapper.selectCount(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getWarehouseId, id));
        if (orderItemCount != null && orderItemCount > 0) {
            refs.add(orderItemCount + " 条订单明细");
        }
        Long checkCount = stockCheckMapper.selectCount(
                new LambdaQueryWrapper<StockCheck>().eq(StockCheck::getWarehouseId, id));
        if (checkCount != null && checkCount > 0) {
            refs.add(checkCount + " 张盘点单");
        }
        Long logCount = inventoryLogMapper.selectCount(
                new LambdaQueryWrapper<InventoryLog>().eq(InventoryLog::getWarehouseId, id));
        if (logCount != null && logCount > 0) {
            refs.add(logCount + " 条出入库流水");
        }

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getWarehouseId, id);
        List<Inventory> inventories = inventoryMapper.selectList(wrapper);
        for (Inventory inv : inventories) {
            int quantity = inv.getQuantity() == null ? 0 : inv.getQuantity();
            int locked = inv.getLockedQuantity() == null ? 0 : inv.getLockedQuantity();
            if (quantity > 0 || locked > 0) {
                refs.add("还有库存（在库 " + quantity + "，锁定 " + locked + "）");
                break;
            }
        }

        if (!refs.isEmpty()) {
            throw new RuntimeException("仓库「" + warehouse.getWarehouseName() + "」不能删除：已被 "
                    + String.join("、", refs)
                    + " 引用，删掉之后这些地方的仓库名会变成 #ID 或空白。不再使用请把状态改为「停用」");
        }

        // 都是 0 的残留库存行没有保留价值，一并清掉
        if (!inventories.isEmpty()) {
            inventoryMapper.delete(wrapper);
        }
        // P043：逻辑删除前把编码让出来，否则 uk_warehouse_code 会被已删记录一直占着
        Warehouse release = new Warehouse();
        release.setId(id);
        release.setWarehouseCode(CodeReleaser.release(warehouse.getWarehouseCode(), id, 50));
        release.setUpdateTime(LocalDateTime.now());
        warehouseMapper.updateById(release);

        return warehouseMapper.deleteById(id) > 0;
    }

    @Override
    public List<Warehouse> listAll() {
        return listAll(true);
    }

    @Override
    public List<Warehouse> listAll(boolean onlyEnabled) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (onlyEnabled) {
            wrapper.eq(Warehouse::getStatus, 1);
        }
        // 仓库编码按自然序（WH-2 在 WH-10 前面）
        wrapper.last(SortUtil.build("warehouseCode", "asc", WAREHOUSE_SORT));
        return warehouseMapper.selectList(wrapper);
    }
}
