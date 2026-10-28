package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockChangeDTO;
import com.hrms.entity.Inventory;
import com.hrms.entity.OrderItem;
import com.hrms.entity.Product;
import com.hrms.entity.Customer;
import com.hrms.entity.Shipment;
import com.hrms.entity.ShipmentTrack;
import com.hrms.entity.TradeOrder;
import com.hrms.mapper.CustomerMapper;
import com.hrms.mapper.OrderItemMapper;
import com.hrms.mapper.ProductMapper;
import com.hrms.mapper.ShipmentMapper;
import com.hrms.mapper.ShipmentTrackMapper;
import com.hrms.mapper.TradeOrderMapper;
import com.hrms.service.InventoryService;
import com.hrms.service.SeventeenTrackService;
import com.hrms.service.ShipmentService;
import com.hrms.util.SortUtil;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
 * 发货物流Service实现类
 */
@Service
public class ShipmentServiceImpl implements ShipmentService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 轨迹时间统一口径，和 17TRACK 返回格式一致 */
    private static final DateTimeFormatter TRACK_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_SHIPPING = 2;
    private static final int STATUS_CUSTOMS = 3;
    private static final int STATUS_SIGNED = 4;

    /** 发货单列表可排序字段白名单（P004） */
    private static final Map<String, SortUtil.OrderCol> SHIPMENT_SORT = new HashMap<>();

    static {
        SHIPMENT_SORT.put("shipmentNo", SortUtil.OrderCol.natural("shipment_no"));
        SHIPMENT_SORT.put("shipDate", SortUtil.OrderCol.plain("ship_date"));
        SHIPMENT_SORT.put("eta", SortUtil.OrderCol.plain("eta"));
        SHIPMENT_SORT.put("status", SortUtil.OrderCol.plain("status"));
        SHIPMENT_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    @Autowired
    private ShipmentMapper shipmentMapper;

    @Autowired
    private ShipmentTrackMapper shipmentTrackMapper;

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
    private SeventeenTrackService seventeenTrackService;

    @Override
    public PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO) {
        Page<Shipment> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Shipment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Shipment::getShipmentNo, queryDTO.getKeyword())
                    .or().like(Shipment::getTrackingNo, queryDTO.getKeyword())
                    .or().like(Shipment::getContainerNo, queryDTO.getKeyword()));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Shipment::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getOrderId() != null) {
            wrapper.eq(Shipment::getOrderId, queryDTO.getOrderId());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), SHIPMENT_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(Shipment::getCreateTime).orderByDesc(Shipment::getId);
        }
        Page<Shipment> result = shipmentMapper.selectPage(page, wrapper);

        // 订单号一次查完，不再每行单查（P017）
        List<Shipment> records = result.getRecords();
        Set<Long> orderIds = records.stream()
                .map(Shipment::getOrderId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, TradeOrder> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : tradeOrderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(TradeOrder::getId, o -> o, (a, b) -> a));

        List<Map<String, Object>> list = records.stream().map(shipment -> {
            Map<String, Object> map = new HashMap<>();
            map.put("shipment", shipment);
            TradeOrder order = orderMap.get(shipment.getOrderId());
            map.put("orderNo", order == null ? "" : order.getOrderNo());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public Shipment getById(Long id) {
        return shipmentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Shipment shipment) {
        if (shipment == null || shipment.getOrderId() == null) {
            throw new RuntimeException("请选择关联订单");
        }
        TradeOrder order = tradeOrderMapper.selectById(shipment.getOrderId());
        if (order == null) {
            throw new RuntimeException("关联订单不存在或已删除");
        }
        if (order.getStatus() != null && order.getStatus() == 9) {
            throw new RuntimeException("该订单已取消，不能再建发货单");
        }

        // 同一订单只允许一张有效发货单（P026）
        LambdaQueryWrapper<Shipment> dup = new LambdaQueryWrapper<>();
        dup.eq(Shipment::getOrderId, shipment.getOrderId());
        if (shipmentMapper.selectCount(dup) > 0) {
            throw new RuntimeException("订单「" + order.getOrderNo() + "」已有发货单，不能重复发货");
        }

        // 建单必填项：只有订单和日期的空单推不动、也查不到，属于假单据
        requireCreateFields(shipment);
        validateLength(shipment);

        if (!StringUtils.hasText(shipment.getShipmentNo())) {
            shipment.setShipmentNo(generateShipmentNo());
        } else if (shipment.getShipmentNo().length() > 50) {
            throw new RuntimeException("发货单号最多 50 个字符");
        }

        if (shipment.getStatus() == null) {
            shipment.setStatus(STATUS_PENDING);
        }
        shipment.setCreateTime(LocalDateTime.now());
        shipment.setUpdateTime(LocalDateTime.now());
        boolean ok = shipmentMapper.insert(shipment) > 0;

        if (ok) {
            addTrack(shipment, STATUS_PENDING, "已建单", null, "发货单创建，等待发货");
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Shipment shipment) {
        if (shipment == null || shipment.getId() == null) {
            throw new RuntimeException("发货单不存在");
        }
        Shipment origin = shipmentMapper.selectById(shipment.getId());
        if (origin == null) {
            throw new RuntimeException("发货单不存在或已删除");
        }
        // 已签收的单据只读：货已交付、库存已出、轨迹已收尾，再改就跟历史对不上了
        int originStatus = origin.getStatus() == null ? STATUS_PENDING : origin.getStatus();
        if (originStatus == STATUS_SIGNED) {
            throw new RuntimeException("发货单「" + origin.getShipmentNo() + "」已签收，不能再修改");
        }
        // 分级可编辑：已发货（运输中/清关中）后，发货日期和运输方式是既定事实不能再改
        guardShippedFields(shipment, origin, originStatus);
        // 建单时的必填项在编辑时也允许为空（历史空单要能补），但不允许把已有值清成空
        guardNotCleared(shipment, origin);
        // 先算出本次真正改动了哪些字段，改完写进轨迹，事后能解释单据为什么和当初不一样
        List<String> changes = diffChanges(shipment, origin);
        // 订单、状态、创建时间都不允许通过编辑接口改：状态只能走 updateStatus 单步推进。
        // 否则编辑框一直开着、别人把状态推进了，自己一保存就把旧状态写回去，等于绕过 P027。
        shipment.setOrderId(null);
        shipment.setStatus(null);
        shipment.setCreateTime(null);
        validateLength(shipment);
        shipment.setUpdateTime(LocalDateTime.now());
        boolean ok = shipmentMapper.updateById(shipment) > 0;
        if (ok && originStatus >= STATUS_SHIPPING && !changes.isEmpty()) {
            addTrack(origin, originStatus, "单据变更", null, joinChanges(changes));
        }
        return ok;
    }

    /**
     * 已发货后的字段白名单校验。
     * 运输中/清关中允许改承运商、跟踪号、集装箱号、起运/目的港、ETD、ETA、运费、备注
     * （临时换船换柜、跟踪号后补、ETA 延误都是真实场景，且都不影响已扣减的库存）；
     * 发货日期和运输方式属于既定事实，改了会和已有轨迹、库存流水对不上。
     */
    private void guardShippedFields(Shipment input, Shipment origin, int originStatus) {
        if (originStatus < STATUS_SHIPPING) {
            return;
        }
        List<String> locked = new ArrayList<>();
        if (input.getShipDate() != null && !input.getShipDate().equals(origin.getShipDate())) {
            locked.add("发货日期");
        }
        if (StringUtils.hasText(input.getTransportType())
                && !input.getTransportType().equals(origin.getTransportType())) {
            locked.add("运输方式");
        }
        if (!locked.isEmpty()) {
            throw new RuntimeException("发货单已处于「" + statusText(originStatus) + "」，"
                    + String.join("、", locked) + "不能修改；"
                    + "已发货后只能改承运商、跟踪号、集装箱号、起运/目的港、ETD、ETA、运费、备注");
        }
    }

    /**
     * 对比出实际发生变化的字段，供写入变更轨迹。
     * 入参为 null 表示本次没有提交该字段（MyBatis-Plus 也不会更新），直接跳过。
     */
    private List<String> diffChanges(Shipment input, Shipment origin) {
        List<String> changes = new ArrayList<>();
        appendChange(changes, "承运商", origin.getCarrier(), input.getCarrier());
        appendChange(changes, "跟踪号", origin.getTrackingNo(), input.getTrackingNo());
        appendChange(changes, "集装箱号", origin.getContainerNo(), input.getContainerNo());
        appendChange(changes, "起运港", origin.getPortFrom(), input.getPortFrom());
        appendChange(changes, "目的港", origin.getPortTo(), input.getPortTo());
        appendChange(changes, "ETD", dateText(origin.getEtd()), dateText(input.getEtd()));
        appendChange(changes, "ETA", dateText(origin.getEta()), dateText(input.getEta()));
        appendChange(changes, "运费(USD)", moneyText(origin.getFreightCostUsd()), moneyText(input.getFreightCostUsd()));
        appendChange(changes, "备注", origin.getRemark(), input.getRemark());
        return changes;
    }

    private void appendChange(List<String> changes, String label, String before, String after) {
        if (after == null) {
            return;
        }
        if (before != null && before.equals(after)) {
            return;
        }
        if (before == null && after.isEmpty()) {
            return;
        }
        changes.add(label + "：" + (StringUtils.hasText(before) ? before : "（空）")
                + " → " + (StringUtils.hasText(after) ? after : "（空）"));
    }

    private String joinChanges(List<String> changes) {
        String text = String.join("；", changes);
        return text.length() > 280 ? text.substring(0, 277) + "..." : text;
    }

    private String dateText(java.time.LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String moneyText(java.math.BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    /**
     * 状态推进
     *
     * 只允许 1→2→3→4 单步前进，不能跳级、不能回退（P027）。
     * 推进到"运输中"时真正出库，并把订单状态联动成"已发货"（P015）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        if (status == null || status < STATUS_PENDING || status > STATUS_SIGNED) {
            throw new RuntimeException("不支持的发货状态");
        }
        Shipment origin = shipmentMapper.selectById(id);
        if (origin == null) {
            throw new RuntimeException("发货单不存在或已删除");
        }
        int fromStatus = origin.getStatus() == null ? STATUS_PENDING : origin.getStatus();

        if (fromStatus == status) {
            throw new RuntimeException("发货单已经是「" + statusText(status) + "」状态");
        }
        if (status != fromStatus + 1) {
            throw new RuntimeException("状态只能按 待发货 → 运输中 → 清关中 → 已签收 依次推进，"
                    + "当前是「" + statusText(fromStatus) + "」，不能直接改成「" + statusText(status) + "」");
        }
        // 推进到「运输中」= 真正出库，先把物流信息补齐再动库存
        if (status == STATUS_SHIPPING) {
            requireFieldsBeforeShip(origin);
        }

        Shipment shipment = new Shipment();
        shipment.setId(id);
        shipment.setStatus(status);
        shipment.setUpdateTime(LocalDateTime.now());
        boolean ok = shipmentMapper.updateById(shipment) > 0;
        if (!ok) {
            return false;
        }

        if (status == STATUS_SHIPPING) {
            deductStockOnShip(origin);
            syncOrderStatus(origin.getOrderId(), 3);
        } else if (status == STATUS_SIGNED) {
            syncOrderStatus(origin.getOrderId(), 4);
        }

        addTrack(origin, status, trackNode(status), trackLocation(origin, status), trackContent(status));
        return true;
    }

    /**
     * 发货时扣减库存：
     * 先把下单时锁定的数量释放掉，再按明细数量出库，两个动作在同一事务里。
     * 历史订单没有锁定量时（locked = 0）直接出库。
     */
    private void deductStockOnShip(Shipment shipment) {
        if (shipment.getOrderId() == null) {
            return;
        }
        TradeOrder order = tradeOrderMapper.selectById(shipment.getOrderId());
        String orderNo = order == null ? null : order.getOrderNo();

        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, shipment.getOrderId());
        List<OrderItem> items = orderItemMapper.selectList(wrapper);

        for (OrderItem item : items) {
            if (item.getWarehouseId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }

            Inventory inventory = inventoryService.getByProductAndWarehouse(item.getProductId(), item.getWarehouseId());
            int locked = (inventory == null || inventory.getLockedQuantity() == null) ? 0 : inventory.getLockedQuantity();

            if (locked > 0) {
                StockChangeDTO unlock = new StockChangeDTO();
                unlock.setProductId(item.getProductId());
                unlock.setWarehouseId(item.getWarehouseId());
                unlock.setChangeType(4);
                unlock.setQuantity(Math.min(locked, item.getQuantity()));
                unlock.setOrderNo(orderNo);
                unlock.setOperator("系统");
                unlock.setRemark("发货释放锁定量");
                inventoryService.changeStock(unlock);
            }

            StockChangeDTO out = new StockChangeDTO();
            out.setProductId(item.getProductId());
            out.setWarehouseId(item.getWarehouseId());
            out.setChangeType(2);
            out.setQuantity(item.getQuantity());
            out.setOrderNo(orderNo);
            out.setOperator("系统");
            out.setRemark("发货出库");
            inventoryService.changeStock(out);
        }
    }

    /**
     * 发货进度回写订单状态（P015）
     * 只往前推，不覆盖已取消(9)和已完成的订单。
     */
    private void syncOrderStatus(Long orderId, int targetStatus) {
        if (orderId == null) {
            return;
        }
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        int current = order.getStatus() == null ? 0 : order.getStatus();
        if (current == 9 || current >= targetStatus) {
            return;
        }
        TradeOrder update = new TradeOrder();
        update.setId(orderId);
        update.setStatus(targetStatus);
        update.setUpdateTime(LocalDateTime.now());
        tradeOrderMapper.updateById(update);
    }

    /**
     * 删除发货单（P028）
     * 已发货的单据不允许删，否则物流记录和已出的库存都对不上。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        Shipment shipment = shipmentMapper.selectById(id);
        if (shipment == null) {
            throw new RuntimeException("发货单不存在或已删除");
        }
        int status = shipment.getStatus() == null ? STATUS_PENDING : shipment.getStatus();
        if (status >= STATUS_SHIPPING) {
            throw new RuntimeException("发货单「" + shipment.getShipmentNo() + "」已处于「"
                    + statusText(status) + "」，不能删除");
        }
        // 逻辑删除：@TableLogic 自动把 deleted 置 1
        return shipmentMapper.deleteById(id) > 0;
    }

    @Override
    public List<ShipmentTrack> listTracks(Long shipmentId) {
        if (shipmentId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ShipmentTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShipmentTrack::getShipmentId, shipmentId)
                .orderByAsc(ShipmentTrack::getTrackTime)
                .orderByAsc(ShipmentTrack::getId);
        return shipmentTrackMapper.selectList(wrapper);
    }

    private void addTrack(Shipment shipment, int status, String node, String location, String content) {
        ShipmentTrack track = new ShipmentTrack();
        track.setShipmentId(shipment.getId());
        track.setStatus(status);
        track.setNode(node);
        track.setLocation(location);
        track.setContent(content);
        track.setTrackTime(LocalDateTime.now());
        track.setOperator(UserContext.getOrDefault("系统"));
        track.setCreateTime(LocalDateTime.now());
        shipmentTrackMapper.insert(track);
    }

    private String trackNode(int status) {
        switch (status) {
            case STATUS_SHIPPING:
                return "已发货";
            case STATUS_CUSTOMS:
                return "清关中";
            case STATUS_SIGNED:
                return "已签收";
            default:
                return "已建单";
        }
    }

    private String trackLocation(Shipment shipment, int status) {
        if (status == STATUS_SHIPPING) {
            return shipment.getPortFrom();
        }
        if (status == STATUS_CUSTOMS || status == STATUS_SIGNED) {
            return shipment.getPortTo();
        }
        return null;
    }

    private String trackContent(int status) {
        switch (status) {
            case STATUS_SHIPPING:
                return "货物已离港/交运，运输中";
            case STATUS_CUSTOMS:
                return "货物到达目的港，正在清关";
            case STATUS_SIGNED:
                return "收货方已签收，物流完成";
            default:
                return "发货单已创建";
        }
    }

    private String statusText(int status) {
        switch (status) {
            case STATUS_PENDING:
                return "待发货";
            case STATUS_SHIPPING:
                return "运输中";
            case STATUS_CUSTOMS:
                return "清关中";
            case STATUS_SIGNED:
                return "已签收";
            default:
                return "未知状态";
        }
    }

    /**
     * 发货单号：SH + 时间戳，撞号时加序号（并发下也不容易冲突）
     */
    private String generateShipmentNo() {
        String base = "SH" + LocalDateTime.now().format(NO_FORMATTER);
        String no = base;
        int seq = 1;
        while (shipmentMapper.countByNoIncludeDeleted(no) > 0) {
            no = base + "-" + seq;
            if (++seq > 99) {
                throw new RuntimeException("发货单号生成失败，请重试");
            }
        }
        return no;
    }

    /**
     * 建单必填项：发货日期、运输方式、承运商、起运港、目的港、ETD。
     * 追踪号/集装箱号发货后才有，ETA 会延误、运费后定，这三项不在这里卡。
     */
    private void requireCreateFields(Shipment s) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(s.getTransportType())) {
            missing.add("运输方式");
        }
        if (!StringUtils.hasText(s.getCarrier())) {
            missing.add("承运商");
        }
        if (!StringUtils.hasText(s.getPortFrom())) {
            missing.add("起运港");
        }
        if (!StringUtils.hasText(s.getPortTo())) {
            missing.add("目的港");
        }
        if (s.getEtd() == null) {
            missing.add("预计离港日期");
        }
        if (!missing.isEmpty()) {
            throw new RuntimeException("请填写完整，还缺：" + String.join("、", missing)
                    + "（追踪号/集装箱号可发货后补，预计到港和运费可后填）");
        }
        if (s.getShipDate() == null) {
            throw new RuntimeException("请选择发货日期");
        }
    }

    /**
     * 推进「运输中」= 真正出库，此时物流信息必须齐全：
     * 承运商、起运港、目的港、ETD，且追踪号与集装箱号至少有一个。
     * 否则库存扣了、轨迹却查不到货，事后无法对账。
     */
    private void requireFieldsBeforeShip(Shipment s) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(s.getCarrier())) {
            missing.add("承运商");
        }
        if (!StringUtils.hasText(s.getPortFrom())) {
            missing.add("起运港");
        }
        if (!StringUtils.hasText(s.getPortTo())) {
            missing.add("目的港");
        }
        if (s.getEtd() == null) {
            missing.add("预计离港日期");
        }
        if (!StringUtils.hasText(s.getTrackingNo()) && !StringUtils.hasText(s.getContainerNo())) {
            missing.add("追踪号或集装箱号（至少填一个）");
        }
        if (!missing.isEmpty()) {
            throw new RuntimeException("发货前请先补齐：" + String.join("、", missing)
                    + "。货走后轨迹要能查到，否则与库存流水对不上");
        }
    }

    /**
     * 已有值不允许被清空，否则「新增必填 → 编辑清空」就能绕过建单校验。
     */
    private void guardNotCleared(Shipment s, Shipment origin) {
        checkNotCleared(origin.getTransportType(), s.getTransportType(), "运输方式");
        checkNotCleared(origin.getCarrier(), s.getCarrier(), "承运商");
        checkNotCleared(origin.getPortFrom(), s.getPortFrom(), "起运港");
        checkNotCleared(origin.getPortTo(), s.getPortTo(), "目的港");
    }

    private void checkNotCleared(String originVal, String newVal, String label) {
        if (StringUtils.hasText(originVal) && newVal != null && !StringUtils.hasText(newVal)) {
            throw new RuntimeException(label + "不能清空，如需变更请填写新值");
        }
    }

    /**
     * 字段长度校验，避免超长直接抛 SQL 错（P029）
     */
    private void validateLength(Shipment s) {
        checkLen(s.getCarrier(), 100, "承运商");
        checkLen(s.getTrackingNo(), 100, "追踪号");
        checkLen(s.getContainerNo(), 50, "集装箱号");
        checkLen(s.getPortFrom(), 100, "起运港");
        checkLen(s.getPortTo(), 100, "目的港");
        checkLen(s.getTransportType(), 20, "运输方式");
        checkLen(s.getRemark(), 500, "备注");
    }

    private void checkLen(String value, int max, String label) {
        if (value != null && value.length() > max) {
            throw new RuntimeException(label + "最多 " + max + " 个字符");
        }
    }

    @Override
    public List<Map<String, Object>> mapRoutes(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<Shipment> wrapper = new LambdaQueryWrapper<Shipment>()
                .isNotNull(Shipment::getTrackingNo)
                .ne(Shipment::getTrackingNo, "");
        // 多字段搜索：发货单号/运单号/承运商/集装箱号/起运目的港，以及订单号/客户/货物名称命中的订单
        if (StringUtils.hasText(kw)) {
            Set<Long> matchedOrderIds = matchOrderIds(kw);
            wrapper.and(w -> {
                w.like(Shipment::getShipmentNo, kw)
                        .or().like(Shipment::getTrackingNo, kw)
                        .or().like(Shipment::getCarrier, kw)
                        .or().like(Shipment::getContainerNo, kw)
                        .or().like(Shipment::getPortFrom, kw)
                        .or().like(Shipment::getPortTo, kw);
                if (!matchedOrderIds.isEmpty()) {
                    w.or().in(Shipment::getOrderId, matchedOrderIds);
                }
            });
        }
        // 只取「已运输/清关中/已签收」且填了追踪号的单，驾驶舱不再画待发货的空线
        List<Shipment> ships = shipmentMapper.selectList(wrapper
                .ge(Shipment::getStatus, STATUS_SHIPPING)
                .orderByDesc(Shipment::getUpdateTime)
                .last("LIMIT 50"));
        if (ships.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = ships.stream().map(Shipment::getId).collect(Collectors.toList());
        List<ShipmentTrack> tracks = shipmentTrackMapper.selectList(new LambdaQueryWrapper<ShipmentTrack>()
                .in(ShipmentTrack::getShipmentId, ids)
                .orderByAsc(ShipmentTrack::getTrackTime)
                .orderByAsc(ShipmentTrack::getId));
        Map<Long, List<ShipmentTrack>> byShip = tracks.stream()
                .collect(Collectors.groupingBy(ShipmentTrack::getShipmentId));

        // 订单号 / 客户 / 货物名称，供面板与 tooltip 显示，便于按“货名”确认是哪一票
        Set<Long> orderIds = ships.stream()
                .map(Shipment::getOrderId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, TradeOrder> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : tradeOrderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(TradeOrder::getId, o -> o, (a, b) -> a));
        Set<Long> customerIds = orderMap.values().stream()
                .map(TradeOrder::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> customerNameMap = customerIds.isEmpty() ? Collections.emptyMap()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId,
                        c -> c.getCompanyName() == null ? "" : c.getCompanyName(), (a, b) -> a));
        List<OrderItem> items = orderIds.isEmpty() ? Collections.emptyList()
                : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds));
        Set<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> productNameMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId,
                        p -> p.getName() == null ? "" : p.getName(), (a, b) -> a));
        Map<Long, String> productNamesByOrder = new HashMap<>();
        for (OrderItem item : items) {
            String name = productNameMap.get(item.getProductId());
            if (name == null || name.isEmpty()) {
                continue;
            }
            productNamesByOrder.merge(item.getOrderId(), name,
                    (old, add) -> old.contains(add) ? old : old + " / " + add);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Shipment s : ships) {
            List<ShipmentTrack> list = byShip.getOrDefault(s.getId(), Collections.emptyList());
            if (list.isEmpty()) {
                // 没有任何真实节点就不画，避免固定进度假航线
                continue;
            }
            ShipmentTrack latest = list.get(list.size() - 1);
            ShipmentTrack located = null;
            for (int i = list.size() - 1; i >= 0; i--) {
                if (StringUtils.hasText(list.get(i).getLocation())) {
                    located = list.get(i);
                    break;
                }
            }
            boolean fromTracker = list.stream().anyMatch(t -> "17TRACK".equals(t.getOperator()));
            TradeOrder order = s.getOrderId() == null ? null : orderMap.get(s.getOrderId());
            Map<String, Object> row = new HashMap<>();
            row.put("id", s.getId());
            row.put("shipmentNo", s.getShipmentNo());
            row.put("trackingNo", s.getTrackingNo());
            row.put("carrier", s.getCarrier());
            row.put("status", s.getStatus());
            row.put("portFrom", s.getPortFrom());
            row.put("portTo", s.getPortTo());
            row.put("orderNo", order == null ? "" : order.getOrderNo());
            row.put("customerName", order == null ? "" : customerNameMap.getOrDefault(order.getCustomerId(), ""));
            row.put("productNames", s.getOrderId() == null ? "" : productNamesByOrder.getOrDefault(s.getOrderId(), ""));
            row.put("trackCount", list.size());
            row.put("trackSource", fromTracker ? "17TRACK" : "本地节点");
            row.put("latestNode", latest.getNode());
            row.put("latestContent", latest.getContent());
            row.put("latestTime", latest.getTrackTime());
            row.put("latestLocation", located == null ? "" : located.getLocation());
            row.put("latestLocatedTime", located == null ? null : located.getTrackTime());
            rows.add(row);
        }
        return rows;
    }

    /**
     * 关键词命中订单号 / 客户名称 / 货物名称时，返回对应订单 id。
     * 这样只知道货名或客户也能找到对应航线。
     */
    private Set<Long> matchOrderIds(String keyword) {
        Set<Long> ids = new LinkedHashSet<>();
        tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                        .like(TradeOrder::getOrderNo, keyword))
                .forEach(o -> ids.add(o.getId()));

        List<Long> customerIds = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                        .like(Customer::getCompanyName, keyword)
                        .or().like(Customer::getContactName, keyword))
                .stream().map(Customer::getId).collect(Collectors.toList());
        if (!customerIds.isEmpty()) {
            tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                            .in(TradeOrder::getCustomerId, customerIds))
                    .forEach(o -> ids.add(o.getId()));
        }

        List<Long> productIds = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .like(Product::getName, keyword)
                        .or().like(Product::getNameEn, keyword)
                        .or().like(Product::getSku, keyword))
                .stream().map(Product::getId).collect(Collectors.toList());
        if (!productIds.isEmpty()) {
            orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                            .in(OrderItem::getProductId, productIds))
                    .forEach(item -> ids.add(item.getOrderId()));
        }
        return ids;
    }

    @Override
    @Transactional
    public int syncLiveTracks(Long shipmentId) {
        Shipment shipment = shipmentMapper.selectById(shipmentId);
        if (shipment == null) {
            throw new RuntimeException("发货单不存在");
        }
        if (!StringUtils.hasText(shipment.getTrackingNo())) {
            throw new RuntimeException("该发货单还没有追踪号，无法同步轨迹");
        }
        List<Map<String, Object>> nodes = seventeenTrackService.query(shipment.getTrackingNo());
        List<ShipmentTrack> existing = listTracks(shipmentId);
        LinkedHashSet<String> exists = existing.stream()
                .map(t -> (t.getContent() == null ? "" : t.getContent()) + "|"
                        + (t.getTrackTime() == null ? "" : t.getTrackTime().format(TRACK_FMT)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int inserted = 0;
        for (Map<String, Object> node : nodes) {
            String content = str(node.get("content"));
            LocalDateTime trackTime = parseTrackTime(str(node.get("trackTime")));
            if (!exists.add(content + "|" + trackTime.format(TRACK_FMT))) {
                continue;
            }
            ShipmentTrack track = new ShipmentTrack();
            track.setShipmentId(shipmentId);
            track.setStatus(inferTrackStatus(str(node.get("node")) + " " + content,
                    shipment.getStatus() == null ? STATUS_SHIPPING : shipment.getStatus()));
            track.setNode(str(node.get("node")));
            track.setLocation(str(node.get("location")));
            track.setContent(content);
            track.setTrackTime(trackTime);
            track.setOperator("17TRACK");
            track.setCreateTime(LocalDateTime.now());
            shipmentTrackMapper.insert(track);
            inserted++;
        }
        return inserted;
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 17TRACK 节点没有统一状态字段，按节点文案归类：
     * 签收→已签收(4)，清关→清关中(3)，揽收/运输/离港/到港/派送→运输中(2)，其余沿用发货单当前状态。
     */
    private int inferTrackStatus(String text, int fallback) {
        if (!StringUtils.hasText(text)) {
            return fallback;
        }
        String lower = text.toLowerCase();
        if (text.contains("签收") || text.contains("已送达") || lower.contains("delivered")) {
            return STATUS_SIGNED;
        }
        if (text.contains("清关") || lower.contains("customs")) {
            return STATUS_CUSTOMS;
        }
        if (text.contains("揽收") || text.contains("运输") || text.contains("离港") || text.contains("到港")
                || text.contains("派送") || text.contains("出发") || text.contains("抵达")
                || lower.contains("picked") || lower.contains("transit") || lower.contains("arrived")
                || lower.contains("depart") || lower.contains("out for delivery")) {
            return STATUS_SHIPPING;
        }
        return fallback;
    }

    /** 17TRACK 时间一般为 yyyy-MM-dd HH:mm:ss；容器事件可能是 ISO 带 T，解析失败就记当前时间 */
    private LocalDateTime parseTrackTime(String text) {
        if (!StringUtils.hasText(text)) {
            return LocalDateTime.now();
        }
        String value = text.trim().replace('T', ' ');
        for (String pattern : new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"}) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
                return pattern.equals("yyyy-MM-dd")
                        ? LocalDateTime.parse(value + " 00:00:00", TRACK_FMT)
                        : LocalDateTime.parse(value, fmt);
            } catch (Exception ignored) {
                // 尝试下一种格式
            }
        }
        return LocalDateTime.now();
    }
}
