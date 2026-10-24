package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockCheckOrderDTO;
import com.hrms.entity.Inventory;
import com.hrms.entity.InventoryLog;
import com.hrms.entity.Product;
import com.hrms.entity.StockCheck;
import com.hrms.entity.StockCheckItem;
import com.hrms.entity.Warehouse;
import com.hrms.mapper.InventoryLogMapper;
import com.hrms.mapper.InventoryMapper;
import com.hrms.mapper.ProductMapper;
import com.hrms.mapper.StockCheckItemMapper;
import com.hrms.mapper.StockCheckMapper;
import com.hrms.mapper.WarehouseMapper;
import com.hrms.service.StockCheckService;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存盘点单Service实现（P019）
 *
 * 关键约束：
 * - 草稿阶段只记快照，不动库存
 * - 确认时在一个事务里逐行加行锁、按"实盘 - 确认时账面"的差异调整，并写流水
 * - 实盘数量不能小于已锁定数量，否则可用量会变负
 */
@Service
public class StockCheckServiceImpl implements StockCheckService {

    private static final int MAX_REMARK_LENGTH = 200;

    private static final int DEFAULT_WARNING_QUANTITY = 100;

    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private StockCheckMapper stockCheckMapper;

    @Autowired
    private StockCheckItemMapper stockCheckItemMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public Map<String, Object> inventoryRows(Long warehouseId) {
        if (warehouseId == null) {
            throw new RuntimeException("请先选择仓库");
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在或已删除");
        }

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getWarehouseId, warehouseId);
        wrapper.orderByAsc(Inventory::getProductId);
        List<Inventory> inventories = inventoryMapper.selectList(wrapper);

        List<Long> productIds = inventories.stream()
                .map(Inventory::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Inventory inv : inventories) {
            Product p = productMap.get(inv.getProductId());
            if (p == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", inv.getProductId());
            row.put("productName", p.getName());
            row.put("sku", p.getSku());
            row.put("unit", p.getUnit());
            row.put("bookQuantity", nvl(inv.getQuantity()));
            row.put("lockedQuantity", nvl(inv.getLockedQuantity()));
            row.put("warningQuantity", nvl(inv.getWarningQuantity()));
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("warehouseId", warehouse.getId());
        result.put("warehouseName", warehouse.getWarehouseName());
        result.put("rows", rows);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StockCheckOrderDTO dto) {
        validate(dto);
        LocalDate checkDate = dto.getCheckDate() == null ? LocalDate.now() : dto.getCheckDate();

        StockCheck check = new StockCheck();
        check.setCheckNo(generateCheckNo(checkDate));
        check.setWarehouseId(dto.getWarehouseId());
        check.setCheckDate(checkDate);
        check.setStatus(StockCheck.STATUS_DRAFT);
        check.setOperator(trim(UserContext.getOrDefault("系统")));
        check.setRemark(trim(dto.getRemark()));
        check.setCreateTime(LocalDateTime.now());
        check.setUpdateTime(LocalDateTime.now());
        stockCheckMapper.insert(check);

        Summary summary = saveItems(check.getId(), check.getWarehouseId(), dto.getItems());
        check.setItemCount(summary.itemCount);
        check.setDiffRows(summary.diffRows);
        check.setDiffSum(summary.diffSum);
        stockCheckMapper.updateById(check);
        return check.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StockCheckOrderDTO dto) {
        if (dto.getId() == null) {
            throw new RuntimeException("缺少盘点单ID");
        }
        validate(dto);
        StockCheck check = requireEditable(dto.getId());

        check.setWarehouseId(dto.getWarehouseId());
        if (dto.getCheckDate() != null) {
            check.setCheckDate(dto.getCheckDate());
        }
        check.setRemark(trim(dto.getRemark()));
        check.setUpdateTime(LocalDateTime.now());

        LambdaQueryWrapper<StockCheckItem> del = new LambdaQueryWrapper<>();
        del.eq(StockCheckItem::getCheckId, check.getId());
        stockCheckItemMapper.delete(del);

        Summary summary = saveItems(check.getId(), check.getWarehouseId(), dto.getItems());
        check.setItemCount(summary.itemCount);
        check.setDiffRows(summary.diffRows);
        check.setDiffSum(summary.diffSum);
        stockCheckMapper.updateById(check);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(Long id) {
        StockCheck check = requireEditable(id);
        String checkNo = check.getCheckNo();
        String operator = trim(UserContext.getOrDefault("系统"));

        LambdaQueryWrapper<StockCheckItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockCheckItem::getCheckId, id);
        List<StockCheckItem> items = stockCheckItemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            throw new RuntimeException("盘点单没有明细，不能确认");
        }

        int diffRows = 0;
        int diffSum = 0;
        int adjusted = 0;

        for (StockCheckItem item : items) {
            int actual = nvl(item.getActualQuantity());
            Inventory inventory = inventoryMapper.selectForUpdate(item.getProductId(), check.getWarehouseId());
            int before;
            int locked;
            if (inventory == null) {
                if (actual == 0) {
                    // 该产品在该仓库没有库存记录，实盘也是 0，无需处理
                    item.setBookQuantity(0);
                    item.setDiff(0);
                    stockCheckItemMapper.updateById(item);
                    continue;
                }
                inventory = new Inventory();
                inventory.setProductId(item.getProductId());
                inventory.setWarehouseId(check.getWarehouseId());
                inventory.setQuantity(0);
                inventory.setLockedQuantity(0);
                inventory.setWarningQuantity(resolveWarningQuantity(item.getProductId()));
                inventory.setUpdateTime(LocalDateTime.now());
                inventoryMapper.insert(inventory);
                before = 0;
                locked = 0;
            } else {
                before = nvl(inventory.getQuantity());
                locked = nvl(inventory.getLockedQuantity());
            }

            if (actual < locked) {
                Product p = productMapper.selectById(item.getProductId());
                String name = p == null ? ("产品" + item.getProductId()) : p.getName();
                throw new RuntimeException("「" + name + "」实盘 " + actual + " 件，小于已锁定 " + locked
                        + " 件，请先处理关联订单或调大实盘数");
            }

            int diff = actual - before;
            // 账面数刷新为"确认这一刻"的实际值，历史单据才和真正发生的调整一致
            item.setBookQuantity(before);
            item.setDiff(diff);
            stockCheckItemMapper.updateById(item);

            if (diff != 0) {
                inventory.setQuantity(actual);
                inventory.setUpdateTime(LocalDateTime.now());
                inventoryMapper.updateById(inventory);

                InventoryLog log = new InventoryLog();
                log.setProductId(item.getProductId());
                log.setWarehouseId(check.getWarehouseId());
                log.setChangeType(diff > 0 ? 1 : 2);
                log.setQuantity(Math.abs(diff));
                log.setBeforeQuantity(before);
                log.setBeforeLockedQuantity(locked);
                log.setAfterQuantity(actual);
                log.setAfterLockedQuantity(locked);
                log.setOperator(operator);
                String rowRemark = StringUtils.hasText(item.getRemark()) ? "，" + item.getRemark().trim() : "";
                log.setRemark(trim("盘点单 " + checkNo + " 调整：" + before + " → " + actual + rowRemark));
                log.setCreateTime(LocalDateTime.now());
                inventoryLogMapper.insert(log);

                adjusted++;
            }
            diffRows += diff == 0 ? 0 : 1;
            diffSum += diff;
        }

        check.setStatus(StockCheck.STATUS_CONFIRMED);
        check.setItemCount(items.size());
        check.setDiffRows(diffRows);
        check.setDiffSum(diffSum);
        check.setConfirmTime(LocalDateTime.now());
        check.setUpdateTime(LocalDateTime.now());
        stockCheckMapper.updateById(check);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkNo", checkNo);
        result.put("itemCount", items.size());
        result.put("diffRows", diffRows);
        result.put("diffSum", diffSum);
        result.put("adjusted", adjusted);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        StockCheck check = stockCheckMapper.selectById(id);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (!Integer.valueOf(StockCheck.STATUS_DRAFT).equals(check.getStatus())) {
            throw new RuntimeException("只有草稿才能作废；已确认的盘点单请用反盘点修正");
        }
        check.setStatus(StockCheck.STATUS_CANCELLED);
        check.setUpdateTime(LocalDateTime.now());
        stockCheckMapper.updateById(check);
    }

    @Override
    public PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO) {
        int pageNum = queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1 ? 1 : queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : Math.min(queryDTO.getPageSize(), 100);

        LambdaQueryWrapper<StockCheck> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(StockCheck::getCheckNo, queryDTO.getKeyword().trim());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(StockCheck::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getWarehouseId() != null) {
            wrapper.eq(StockCheck::getWarehouseId, queryDTO.getWarehouseId());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(StockCheck::getCheckDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(StockCheck::getCheckDate, queryDTO.getEndDate());
        }
        wrapper.orderByDesc(StockCheck::getCheckDate).orderByDesc(StockCheck::getId);

        Page<StockCheck> page = stockCheckMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<StockCheck> records = page.getRecords();

        Map<Long, String> warehouseNames = warehouseNameMap(records.stream()
                .map(StockCheck::getWarehouseId).distinct().collect(Collectors.toList()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (StockCheck check : records) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", check.getId());
            map.put("checkNo", check.getCheckNo());
            map.put("warehouseId", check.getWarehouseId());
            map.put("warehouseName", warehouseNames.getOrDefault(check.getWarehouseId(), "（仓库已删除）"));
            map.put("checkDate", check.getCheckDate());
            map.put("status", check.getStatus());
            map.put("itemCount", nvl(check.getItemCount()));
            map.put("diffRows", nvl(check.getDiffRows()));
            map.put("diffSum", nvl(check.getDiffSum()));
            map.put("operator", check.getOperator());
            map.put("remark", check.getRemark());
            map.put("confirmTime", check.getConfirmTime());
            map.put("createTime", check.getCreateTime());
            list.add(map);
        }
        return new PageResult<>(page.getTotal(), list);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        StockCheck check = stockCheckMapper.selectById(id);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        Warehouse warehouse = warehouseMapper.selectById(check.getWarehouseId());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("id", check.getId());
        header.put("checkNo", check.getCheckNo());
        header.put("warehouseId", check.getWarehouseId());
        header.put("warehouseName", warehouse == null ? "（仓库已删除）" : warehouse.getWarehouseName());
        header.put("checkDate", check.getCheckDate());
        header.put("status", check.getStatus());
        header.put("itemCount", nvl(check.getItemCount()));
        header.put("diffRows", nvl(check.getDiffRows()));
        header.put("diffSum", nvl(check.getDiffSum()));
        header.put("operator", check.getOperator());
        header.put("remark", check.getRemark());
        header.put("confirmTime", check.getConfirmTime());
        header.put("createTime", check.getCreateTime());

        LambdaQueryWrapper<StockCheckItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockCheckItem::getCheckId, id);
        wrapper.orderByAsc(StockCheckItem::getId);
        List<StockCheckItem> items = stockCheckItemMapper.selectList(wrapper);

        Map<Long, Product> productMap = items.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(items.stream()
                    .map(StockCheckItem::getProductId).distinct().collect(Collectors.toList())).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        List<Map<String, Object>> itemList = new ArrayList<>();
        for (StockCheckItem item : items) {
            Product p = productMap.get(item.getProductId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("productId", item.getProductId());
            row.put("productName", p == null ? ("产品" + item.getProductId()) : p.getName());
            row.put("sku", p == null ? "" : p.getSku());
            row.put("unit", p == null ? "" : p.getUnit());
            row.put("bookQuantity", nvl(item.getBookQuantity()));
            row.put("actualQuantity", nvl(item.getActualQuantity()));
            row.put("diff", nvl(item.getDiff()));
            row.put("remark", item.getRemark());
            itemList.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("check", header);
        result.put("items", itemList);
        return result;
    }

    // ==================== 内部方法 ====================

    private void validate(StockCheckOrderDTO dto) {
        if (dto == null || dto.getWarehouseId() == null) {
            throw new RuntimeException("请选择盘点仓库");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("盘点单至少要有一行明细");
        }
        if (dto.getRemark() != null && dto.getRemark().length() > MAX_REMARK_LENGTH) {
            throw new RuntimeException("备注最多 " + MAX_REMARK_LENGTH + " 个字符");
        }
        for (StockCheckOrderDTO.Item item : dto.getItems()) {
            if (item.getProductId() == null) {
                throw new RuntimeException("明细里存在未选产品的行");
            }
            if (item.getActualQuantity() != null && item.getActualQuantity() < 0) {
                throw new RuntimeException("实盘数量不能为负数");
            }
            if (item.getRemark() != null && item.getRemark().length() > MAX_REMARK_LENGTH) {
                throw new RuntimeException("明细备注最多 " + MAX_REMARK_LENGTH + " 个字符");
            }
        }
    }

    private StockCheck requireEditable(Long id) {
        StockCheck check = stockCheckMapper.selectById(id);
        if (check == null) {
            throw new RuntimeException("盘点单不存在");
        }
        if (!Integer.valueOf(StockCheck.STATUS_DRAFT).equals(check.getStatus())) {
            throw new RuntimeException("该盘点单不是草稿状态，不能修改或确认");
        }
        return check;
    }

    /**
     * 整单重写明细；账面数用建单/改单那一刻的库存做快照，确认时会再刷新
     */
    private Summary saveItems(Long checkId, Long warehouseId, List<StockCheckOrderDTO.Item> items) {
        Summary summary = new Summary();
        for (StockCheckOrderDTO.Item src : items) {
            LambdaQueryWrapper<Inventory> invWrapper = new LambdaQueryWrapper<>();
            invWrapper.eq(Inventory::getProductId, src.getProductId());
            invWrapper.eq(Inventory::getWarehouseId, warehouseId);
            Inventory inv = inventoryMapper.selectOne(invWrapper);

            int book = inv == null ? 0 : nvl(inv.getQuantity());
            int actual = src.getActualQuantity() == null ? book : src.getActualQuantity();
            int diff = actual - book;

            StockCheckItem item = new StockCheckItem();
            item.setCheckId(checkId);
            item.setProductId(src.getProductId());
            item.setBookQuantity(book);
            item.setActualQuantity(actual);
            item.setDiff(diff);
            item.setRemark(trim(src.getRemark()));
            stockCheckItemMapper.insert(item);

            summary.itemCount++;
            if (diff != 0) {
                summary.diffRows++;
            }
            summary.diffSum += diff;
        }
        return summary;
    }

    private String generateCheckNo(LocalDate date) {
        String prefix = "PD" + date.format(NO_DATE) + "-";
        LambdaQueryWrapper<StockCheck> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.likeRight(StockCheck::getCheckNo, prefix);
        long seq = stockCheckMapper.selectCount(countWrapper) + 1;
        for (int i = 0; i < 200; i++) {
            String no = prefix + String.format("%04d", seq + i);
            LambdaQueryWrapper<StockCheck> exists = new LambdaQueryWrapper<>();
            exists.eq(StockCheck::getCheckNo, no);
            if (stockCheckMapper.selectCount(exists) == 0) {
                return no;
            }
        }
        throw new RuntimeException("盘点单号生成失败，请稍后重试");
    }

    private int resolveWarningQuantity(Long productId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getProductId, productId).last("limit 1");
        Inventory any = inventoryMapper.selectOne(wrapper);
        return any != null && any.getWarningQuantity() != null
                ? any.getWarningQuantity() : DEFAULT_WARNING_QUANTITY;
    }

    private Map<Long, String> warehouseNameMap(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new HashMap<>();
        for (Warehouse w : warehouseMapper.selectBatchIds(ids)) {
            map.put(w.getId(), w.getWarehouseName());
        }
        return map;
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static String trim(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > MAX_REMARK_LENGTH ? t.substring(0, MAX_REMARK_LENGTH) : t;
    }

    private static class Summary {
        private int itemCount;
        private int diffRows;
        private int diffSum;
    }
}
