package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.OrderCreateDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockChangeDTO;
import com.hrms.entity.Customer;
import com.hrms.entity.Inventory;
import com.hrms.entity.OrderItem;
import com.hrms.entity.Product;
import com.hrms.entity.Shipment;
import com.hrms.entity.TradeOrder;
import com.hrms.mapper.CustomerMapper;
import com.hrms.mapper.OrderItemMapper;
import com.hrms.mapper.ProductMapper;
import com.hrms.mapper.ShipmentMapper;
import com.hrms.mapper.TradeOrderMapper;
import com.hrms.service.InventoryService;
import com.hrms.service.TradeOrderService;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销售订单Service实现类
 */
@Service
public class TradeOrderServiceImpl implements TradeOrderService {

    /** 订单列表可排序字段白名单（P004） */
    private static final Map<String, SortUtil.OrderCol> ORDER_SORT = new HashMap<>();

    static {
        ORDER_SORT.put("orderNo", SortUtil.OrderCol.natural("order_no"));
        ORDER_SORT.put("orderDate", SortUtil.OrderCol.plain("order_date"));
        ORDER_SORT.put("totalQuantity", SortUtil.OrderCol.plain("total_quantity"));
        ORDER_SORT.put("totalAmountUsd", SortUtil.OrderCol.plain("total_amount_usd"));
        ORDER_SORT.put("status", SortUtil.OrderCol.plain("status"));
        ORDER_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    /* ===== 订单状态（P044） ===== */
    private static final int ST_PENDING = 0;    // 待确认
    private static final int ST_CONFIRMED = 1;  // 已确认
    private static final int ST_PREPARING = 2;  // 备货中
    private static final int ST_SHIPPED = 3;    // 已发货
    private static final int ST_SIGNED = 4;     // 已签收
    private static final int ST_DONE = 5;       // 已完成
    private static final int ST_CANCELLED = 9;  // 已取消

    private static final Map<Integer, String> STATUS_TEXT = new HashMap<>();

    /**
     * 允许的状态流转：key 是当前状态，value 是能改成的状态集合。
     * 5（已完成）和 9（已取消）是终态，不在 key 里，等于「不能再改」。
     */
    private static final Map<Integer, Set<Integer>> TRANSITIONS = new HashMap<>();

    static {
        STATUS_TEXT.put(ST_PENDING, "待确认");
        STATUS_TEXT.put(ST_CONFIRMED, "已确认");
        STATUS_TEXT.put(ST_PREPARING, "备货中");
        STATUS_TEXT.put(ST_SHIPPED, "已发货");
        STATUS_TEXT.put(ST_SIGNED, "已签收");
        STATUS_TEXT.put(ST_DONE, "已完成");
        STATUS_TEXT.put(ST_CANCELLED, "已取消");

        // 未发货前可以取消；已发货(3)之后货已经出库、锁定量也释放了，只能继续往前走
        TRANSITIONS.put(ST_PENDING, new HashSet<>(Arrays.asList(ST_CONFIRMED, ST_CANCELLED)));
        // 「已发货」只能由发货单推进产生（见 ShipmentServiceImpl 回写订单状态），
        // 手工从「已确认」直接跳到「已发货」会跳过出库，物流跟踪页却是空的，两边对不上
        TRANSITIONS.put(ST_CONFIRMED, new HashSet<>(Arrays.asList(ST_PREPARING, ST_CANCELLED)));
        TRANSITIONS.put(ST_PREPARING, new HashSet<>(Arrays.asList(ST_SHIPPED, ST_CANCELLED)));
        TRANSITIONS.put(ST_SHIPPED, new HashSet<>(Collections.singletonList(ST_SIGNED)));
        TRANSITIONS.put(ST_SIGNED, new HashSet<>(Collections.singletonList(ST_DONE)));
    }

    private static String statusText(Integer status) {
        return STATUS_TEXT.getOrDefault(status, "未知(" + status + ")");
    }

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ShipmentMapper shipmentMapper;

    @Override
    public PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO) {
        Page<TradeOrder> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(TradeOrder::getOrderNo, queryDTO.getKeyword());
        }
        if (queryDTO.getCustomerId() != null) {
            wrapper.eq(TradeOrder::getCustomerId, queryDTO.getCustomerId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(TradeOrder::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(TradeOrder::getOrderDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(TradeOrder::getOrderDate, queryDTO.getEndDate());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), ORDER_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(TradeOrder::getOrderDate).orderByDesc(TradeOrder::getId);
        }
        Page<TradeOrder> result = tradeOrderMapper.selectPage(page, wrapper);

        // 一次把客户查出来，避免每行单查（P017）
        List<TradeOrder> records = result.getRecords();
        Set<Long> customerIds = records.stream()
                .map(TradeOrder::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Customer> customerMap = customerIds.isEmpty() ? Collections.emptyMap()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));

        List<Map<String, Object>> list = records.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("order", order);
            Customer customer = customerMap.get(order.getCustomerId());
            map.put("customerName", customer == null ? "" : customer.getCompanyName());
            map.put("customerCountry", customer == null ? "" : customer.getCountry());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public TradeOrder getById(Long id) {
        return tradeOrderMapper.selectById(id);
    }

    @Override
    public List<OrderItem> getItems(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        // 附加产品信息
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                item.setUnitPriceUsd(item.getUnitPriceUsd());
            }
        }
        return items;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder createOrder(OrderCreateDTO dto) {
        TradeOrder order = dto.getOrder();
        List<OrderItem> items = dto.getItems();
        if (order == null) {
            throw new RuntimeException("订单信息不完整");
        }
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("请至少添加一条订单明细");
        }
        Validators.notNull(order.getCustomerId(), "客户");

        // 外部系统/Excel 导入可以指定订单号（沿用对方单号便于对账）；
        // 页面手工建单不传，按 SO + 年月日 + 4 位序号生成（见 generateOrderNo）
        boolean presetNo = StringUtils.hasText(order.getOrderNo());
        if (presetNo) {
            if (tradeOrderMapper.countByNoIncludeDeleted(order.getOrderNo()) > 0) {
                throw new RuntimeException("订单号「" + order.getOrderNo() + "」已存在");
            }
        } else {
            order.setOrderNo(generateOrderNo());
        }
        String orderNo = order.getOrderNo();

        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        if (order.getStatus() == null) {
            order.setStatus(0);
        }
        if (order.getPaymentStatus() == null) {
            order.setPaymentStatus(0);
        }
        if (order.getCurrency() == null) {
            order.setCurrency("USD");
        }
        if (order.getExchangeRate() == null) {
            order.setExchangeRate(new BigDecimal("7.2000"));
        }

        // 计算总量总额
        int totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("明细数量无效");
            }
            BigDecimal price = item.getUnitPriceUsd();
            if (price == null) {
                Product product = productMapper.selectById(item.getProductId());
                if (product == null) {
                    throw new RuntimeException("产品不存在: " + item.getProductId());
                }
                price = product.getSalePriceUsd();
                item.setUnitPriceUsd(price);
            }
            item.setTotalPriceUsd(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            totalQty += item.getQuantity();
            totalAmount = totalAmount.add(item.getTotalPriceUsd());
        }
        order.setTotalQuantity(totalQty);
        order.setTotalAmountUsd(totalAmount);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 两个人同时建单时仍可能抢到同一个号，撞了 uk_order_no 就重新取号再插
        int attempts = 0;
        while (true) {
            try {
                tradeOrderMapper.insert(order);
                break;
            } catch (DuplicateKeyException e) {
                // 外部指定单号撞车不重试：重试会换成另一个号，对方系统就对不上了
                if (presetNo) {
                    throw new RuntimeException("订单号「" + order.getOrderNo() + "」已存在");
                }
                if (++attempts >= 5) {
                    throw new RuntimeException("订单号生成冲突，请重新提交");
                }
                order.setId(null);
                order.setOrderNo(generateOrderNo());
            }
        }
        orderNo = order.getOrderNo();

        // 保存明细并锁定库存
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
            // 仓库不允许为空：空了后面锁定/扣减库存都是静默跳过，
            // 账面库存永远不动，盘点时才发现对不上
            if (item.getWarehouseId() == null) {
                throw new RuntimeException("明细第 " + (items.indexOf(item) + 1)
                        + " 行未选择发货仓库，无法锁定库存");
            }
            StockChangeDTO stock = new StockChangeDTO();
            stock.setProductId(item.getProductId());
            stock.setWarehouseId(item.getWarehouseId());
            stock.setChangeType(3);
            stock.setQuantity(item.getQuantity());
            stock.setOrderNo(orderNo);
            stock.setOperator("系统");
            stock.setRemark("订单锁定库存");
            inventoryService.changeStock(stock);
        }
        return order;
    }

    /**
     * 生成订单号：SO + 年月日 + 4 位当日序号（如 SO20261024-0003）。
     *
     * 序号不能按「未删除条数 + 1」算：订单是逻辑删除，删掉一张后条数变少，
     * 算出来的号会撞上已删记录仍然占着的 uk_order_no，报「订单号已存在」，
     * 而用户在列表里根本看不到那张已删的单。
     * 现在改为取含已删除记录在内的最大号 +1，并对非纯数字后缀、已占用号做跳过。
     */
    private String generateOrderNo() {
        String prefix = "SO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String max = tradeOrderMapper.maxOrderNoIncludeDeleted(prefix);

        int seq = 0;
        if (max != null && max.length() > prefix.length()) {
            String tail = max.substring(prefix.length()).trim();
            if (tail.matches("\\d{1,6}")) {
                seq = Integer.parseInt(tail);
            }
        }

        for (int i = 0; i < 500; i++) {
            String no = prefix + String.format("%04d", seq + 1 + i);
            if (tradeOrderMapper.countByNoIncludeDeleted(no) == 0) {
                return no;
            }
        }
        throw new RuntimeException("订单号生成失败，请重新提交");
    }

    /**
     * 变更订单状态（P044）
     *
     * 以前只把传入的整数直接写库，既不校验状态合法区间也不校验流转顺序：
     * 可以把「待确认」直接改成「已完成」，也可以把「已取消」改回「已确认」；
     * 而取消(9) 时释放掉的锁定量不会因为改回来而重新锁定，之后发货出库时可用量虚高，
     * 可能把别的订单锁着的货出掉。
     *
     * 现在：只认 0/1/2/3/4/5/9；只能按 待确认→已确认→备货中/已发货→已签收→已完成 推进；
     * 取消只允许在未发货（0/1/2）时做，且订单上不能还挂着发货单；5、9 是终态，不可再改。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        if (status == null) {
            throw new RuntimeException("订单状态不能为空");
        }
        if (!STATUS_TEXT.containsKey(status)) {
            throw new RuntimeException("不支持的订单状态：" + status);
        }
        TradeOrder origin = tradeOrderMapper.selectById(id);
        if (origin == null) {
            throw new RuntimeException("订单不存在或已删除");
        }

        int from = origin.getStatus() == null ? ST_PENDING : origin.getStatus();
        if (from == status) {
            return true;
        }
        if (from == ST_CANCELLED) {
            throw new RuntimeException("订单已取消，状态不能再变更；如需继续请重新下单");
        }
        if (from == ST_DONE) {
            throw new RuntimeException("订单已完成，状态不能再变更");
        }
        Set<Integer> allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(status)) {
            throw new RuntimeException("订单状态只能按 待确认 → 已确认 → 备货中/已发货 → 已签收 → 已完成 依次推进，"
                    + "当前是「" + statusText(from) + "」，不能改成「" + statusText(status) + "」");
        }
        // 取消前先确认没有发货单：否则订单取消了、发货单还挂着，后面一推「运输中」照样出库扣库存
        if (status == ST_CANCELLED) {
            guardNoShipment(origin);
        }
        // 收款与订单状态此前各走各的：款没收到也能点「已完成」，两边数据长期对不上。
        // 这里在推进到终态前校验收款状态，避免形成「已完成但未收款」的脏数据。
        if (status == ST_DONE && origin.getPaymentStatus() != null && origin.getPaymentStatus() != 2) {
            throw new RuntimeException("订单尚未收全款，不能标记已完成；请先在收付款页登记收款");
        }

        TradeOrder order = new TradeOrder();
        order.setId(id);
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        boolean ok = tradeOrderMapper.updateById(order) > 0;

        // 订单取消：释放下单时锁定的库存，避免锁定量一直挂着
        if (ok && status == ST_CANCELLED) {
            releaseLockedStock(origin, "订单取消释放锁定量");
        }
        return ok;
    }

    /** 已有发货单的订单不能取消 */
    private void guardNoShipment(TradeOrder order) {
        LambdaQueryWrapper<Shipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shipment::getOrderId, order.getId());
        List<Shipment> shipments = shipmentMapper.selectList(wrapper);
        if (shipments.isEmpty()) {
            return;
        }
        String nos = shipments.stream()
                .map(s -> s.getShipmentNo() == null ? ("#" + s.getId()) : s.getShipmentNo())
                .collect(Collectors.joining("、"));
        throw new RuntimeException("订单「" + order.getOrderNo() + "」已有发货单（" + nos
                + "），不能取消。请先删除未发货的发货单，或改为继续履约");
    }

    @Override
    public boolean update(TradeOrder order) {
        if (order.getId() == null) {
            throw new RuntimeException("订单不存在");
        }
        TradeOrder origin = tradeOrderMapper.selectById(order.getId());
        if (origin == null) {
            throw new RuntimeException("订单不存在或已删除");
        }
        Validators.notCleared(origin.getCustomerId(), order.getCustomerId(), "客户");
        // 单号、数量、金额都由明细汇总而来，不允许通过编辑接口直接改
        order.setOrderNo(null);
        order.setTotalQuantity(null);
        order.setTotalAmountUsd(null);
        // 状态只能走 updateStatus（P044），付款状态由付款记录汇总（P047）；
        // 否则从编辑接口塞个 status 就能绕过流转校验
        order.setStatus(null);
        order.setPaymentStatus(null);
        order.setUpdateTime(LocalDateTime.now());
        return tradeOrderMapper.updateById(order) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        TradeOrder origin = tradeOrderMapper.selectById(id);
        // 订单还没取消就删掉，同样要把锁定的库存释放掉
        boolean wasCancelled = origin != null && origin.getStatus() != null && origin.getStatus() == 9;
        if (origin != null && !wasCancelled) {
            releaseLockedStock(origin, "订单删除释放锁定量");
        }

        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, id);
        orderItemMapper.delete(wrapper);
        return tradeOrderMapper.deleteById(id) > 0;
    }

    /**
     * 释放订单明细占用的锁定量。
     * 只释放实际锁着的数量，历史订单（locked = 0）或无仓库的明细直接跳过。
     */
    private void releaseLockedStock(TradeOrder order, String remark) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemMapper.selectList(wrapper);

        for (OrderItem item : items) {
            if (item.getWarehouseId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            Inventory inventory = inventoryService.getByProductAndWarehouse(item.getProductId(), item.getWarehouseId());
            int locked = (inventory == null || inventory.getLockedQuantity() == null) ? 0 : inventory.getLockedQuantity();
            if (locked <= 0) {
                continue;
            }

            StockChangeDTO dto = new StockChangeDTO();
            dto.setProductId(item.getProductId());
            dto.setWarehouseId(item.getWarehouseId());
            dto.setChangeType(4);
            dto.setQuantity(Math.min(locked, item.getQuantity()));
            dto.setOrderNo(order.getOrderNo());
            dto.setOperator("系统");
            dto.setRemark(remark);
            inventoryService.changeStock(dto);
        }
    }
}
