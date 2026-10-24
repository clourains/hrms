package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Inventory;
import com.hrms.entity.InventoryLog;
import com.hrms.entity.OrderItem;
import com.hrms.entity.Product;
import com.hrms.entity.StockCheckItem;
import com.hrms.mapper.InventoryLogMapper;
import com.hrms.mapper.InventoryMapper;
import com.hrms.mapper.OrderItemMapper;
import com.hrms.mapper.ProductMapper;
import com.hrms.mapper.StockCheckItemMapper;
import com.hrms.service.ProductService;
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
 * 产品Service实现类
 */
@Service
public class ProductServiceImpl implements ProductService {

    /** 产品列表可排序字段白名单（P004） */
    private static final Map<String, SortUtil.OrderCol> PRODUCT_SORT = new HashMap<>();

    static {
        PRODUCT_SORT.put("sku", SortUtil.OrderCol.natural("sku"));
        PRODUCT_SORT.put("name", SortUtil.OrderCol.natural("name"));
        PRODUCT_SORT.put("category", SortUtil.OrderCol.natural("category"));
        PRODUCT_SORT.put("costPrice", SortUtil.OrderCol.plain("cost_price"));
        PRODUCT_SORT.put("salePriceUsd", SortUtil.OrderCol.plain("sale_price_usd"));
        PRODUCT_SORT.put("status", SortUtil.OrderCol.plain("status"));
        PRODUCT_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private StockCheckItemMapper stockCheckItemMapper;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Override
    public PageResult<Product> pageQuery(BizQueryDTO queryDTO) {
        Page<Product> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Product::getName, queryDTO.getKeyword())
                    .or().like(Product::getNameEn, queryDTO.getKeyword())
                    .or().like(Product::getSku, queryDTO.getKeyword())
                    .or().like(Product::getCategory, queryDTO.getKeyword()));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Product::getStatus, queryDTO.getStatus());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), PRODUCT_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }
        Page<Product> result = productMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public boolean save(Product product) {
        Validators.text(product.getSku(), "SKU 编码");
        Validators.text(product.getName(), "产品名称");
        Validators.nonNegative(product.getCostPrice(), "成本价");
        Validators.nonNegative(product.getSalePriceUsd(), "销售价");
        if (!StringUtils.hasText(product.getUnit())) {
            product.setUnit("pcs");
        }
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        return productMapper.insert(product) > 0;
    }

    @Override
    public boolean update(Product product) {
        if (product.getId() == null) {
            throw new RuntimeException("产品不存在");
        }
        Product origin = productMapper.selectById(product.getId());
        if (origin == null) {
            throw new RuntimeException("产品不存在或已删除");
        }
        // 必填项允许留空不改（历史数据），但不允许把已有值清成空
        Validators.notCleared(origin.getSku(), product.getSku(), "SKU 编码");
        Validators.notCleared(origin.getName(), product.getName(), "产品名称");
        Validators.nonNegative(product.getCostPrice(), "成本价");
        Validators.nonNegative(product.getSalePriceUsd(), "销售价");
        product.setUpdateTime(LocalDateTime.now());
        return productMapper.updateById(product) > 0;
    }

    /**
     * 删除产品（P043、P050）
     *
     * P050：产品被别处引用时不允许删除。产品是逻辑删除，@TableLogic 会把已删记录过滤掉，
     * 而订单明细、盘点单明细、库存总览、出入库流水都是拿 product_id 现查名字的，
     * 删完这些地方的产品名就变成 `#ID` 或空白，历史数据再也对不上。
     * 不再销售的产品应该改状态为「下架」（列表仍可见、历史仍可查），而不是删掉。
     *
     * P043：逻辑删除只是把 deleted 置 1，行还在，唯一索引 uk_sku 仍被这条已删记录占着，
     * 同一 SKU 再也建不出来。删之前先把 SKU 让出来。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("产品不存在或已删除");
        }

        List<String> refs = new ArrayList<>();

        Long orderItemCount = orderItemMapper.selectCount(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getProductId, id));
        if (orderItemCount != null && orderItemCount > 0) {
            refs.add(orderItemCount + " 条订单明细");
        }
        Long checkItemCount = stockCheckItemMapper.selectCount(
                new LambdaQueryWrapper<StockCheckItem>().eq(StockCheckItem::getProductId, id));
        if (checkItemCount != null && checkItemCount > 0) {
            refs.add(checkItemCount + " 条盘点单明细");
        }
        Long logCount = inventoryLogMapper.selectCount(
                new LambdaQueryWrapper<InventoryLog>().eq(InventoryLog::getProductId, id));
        if (logCount != null && logCount > 0) {
            refs.add(logCount + " 条出入库流水");
        }

        LambdaQueryWrapper<Inventory> invWrapper = new LambdaQueryWrapper<>();
        invWrapper.eq(Inventory::getProductId, id);
        List<Inventory> inventories = inventoryMapper.selectList(invWrapper);
        for (Inventory inv : inventories) {
            int quantity = inv.getQuantity() == null ? 0 : inv.getQuantity();
            int locked = inv.getLockedQuantity() == null ? 0 : inv.getLockedQuantity();
            if (quantity > 0 || locked > 0) {
                refs.add("还有库存（在库 " + quantity + "，锁定 " + locked + "）");
                break;
            }
        }

        if (!refs.isEmpty()) {
            throw new RuntimeException("产品「" + product.getName() + "」不能删除：已被 "
                    + String.join("、", refs)
                    + " 引用，删掉之后这些地方的产品名会变成 #ID 或空白。不再销售请把状态改为「下架」");
        }

        // 没有任何引用时，全 0 的残留库存行一并清掉，避免留下孤儿行
        if (!inventories.isEmpty()) {
            inventoryMapper.delete(invWrapper);
        }

        Product release = new Product();
        release.setId(id);
        release.setSku(CodeReleaser.release(product.getSku(), id, 50));
        release.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(release);

        return productMapper.deleteById(id) > 0;
    }

    @Override
    public List<Product> listAll() {
        return listAll(false);
    }

    /**
     * includeDisabled = true 时连下架产品一起返回。
     * 出入库、订单明细的下拉必须带上下架产品，否则历史库存行点"入库"时，
     * 下拉里找不到对应选项，产品框会显示成裸 ID（P021）。
     */
    @Override
    public List<Product> listAll(boolean includeDisabled) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (!includeDisabled) {
            wrapper.eq(Product::getStatus, 1);
        }
        // SKU 按自然序（SKU-2 在 SKU-10 前面），下拉框顺序与列表一致
        wrapper.last(SortUtil.build("sku", "asc", PRODUCT_SORT));
        return productMapper.selectList(wrapper);
    }
}
