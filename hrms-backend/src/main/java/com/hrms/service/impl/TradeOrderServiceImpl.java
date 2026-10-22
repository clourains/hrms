package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.OrderCreateDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.StockChangeDTO;
import com.hrms.entity.Customer;
import com.hrms.entity.OrderItem;
import com.hrms.entity.Product;
import com.hrms.entity.TradeOrder;
import com.hrms.mapper.CustomerMapper;
import com.hrms.mapper.OrderItemMapper;
import com.hrms.mapper.ProductMapper;
import com.hrms.mapper.TradeOrderMapper;
import com.hrms.service.InventoryService;
import com.hrms.service.TradeOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 销售订单Service实现类
 */
@Service
public class TradeOrderServiceImpl implements TradeOrderService {

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
        wrapper.orderByDesc(TradeOrder::getOrderDate).orderByDesc(TradeOrder::getId);
        Page<TradeOrder> result = tradeOrderMapper.selectPage(page, wrapper);

        List<Map<String, Object>> list = result.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("order", order);
            Customer customer = customerMapper.selectById(order.getCustomerId());
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
        if (order == null || items == null || items.isEmpty()) {
            throw new RuntimeException("订单信息不完整");
        }

        // 生成订单号 SO + 年月日 + 4位序号
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(TradeOrder::getOrderNo, "SO" + datePart);
        Long count = tradeOrderMapper.selectCount(wrapper);
        String orderNo = "SO" + datePart + "-" + String.format("%04d", count + 1);
        order.setOrderNo(orderNo);

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
        tradeOrderMapper.insert(order);

        // 保存明细并锁定库存
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
            if (item.getWarehouseId() != null) {
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
        }
        return order;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        TradeOrder order = new TradeOrder();
        order.setId(id);
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        return tradeOrderMapper.updateById(order) > 0;
    }

    @Override
    public boolean update(TradeOrder order) {
        order.setUpdateTime(LocalDateTime.now());
        return tradeOrderMapper.updateById(order) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, id);
        orderItemMapper.delete(wrapper);
        return tradeOrderMapper.deleteById(id) > 0;
    }
}
