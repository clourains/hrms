package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hrms.entity.Customer;
import com.hrms.entity.Inventory;
import com.hrms.entity.Product;
import com.hrms.entity.TradeOrder;
import com.hrms.mapper.CustomerMapper;
import com.hrms.mapper.InventoryMapper;
import com.hrms.mapper.OrderItemMapper;
import com.hrms.mapper.PaymentMapper;
import com.hrms.mapper.ProductMapper;
import com.hrms.mapper.TradeOrderMapper;
import com.hrms.mapper.WarehouseMapper;
import com.hrms.service.BusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 营业分析Service实现类
 */
@Service
public class BusinessServiceImpl implements BusinessService {

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>();

        // 有效订单（排除已取消）
        LambdaQueryWrapper<TradeOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.ne(TradeOrder::getStatus, 9);
        List<TradeOrder> orders = tradeOrderMapper.selectList(orderWrapper);

        BigDecimal totalSales = orders.stream()
                .map(o -> o.getTotalAmountUsd() == null ? BigDecimal.ZERO : o.getTotalAmountUsd())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalSalesUsd", totalSales);
        result.put("totalOrders", orders.size());

        // 本年销售额
        int year = LocalDate.now().getYear();
        BigDecimal yearSales = orders.stream()
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().getYear() == year)
                .map(o -> o.getTotalAmountUsd() == null ? BigDecimal.ZERO : o.getTotalAmountUsd())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("yearSalesUsd", yearSales);

        // 客户数
        Long customerCount = customerMapper.selectCount(new LambdaQueryWrapper<>());
        result.put("totalCustomers", customerCount);

        // 库存总值（按成本价折算）
        List<Inventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal inventoryValue = BigDecimal.ZERO;
        for (Inventory inv : inventories) {
            Product product = productMapper.selectById(inv.getProductId());
            if (product != null && product.getCostPrice() != null && inv.getQuantity() != null) {
                inventoryValue = inventoryValue.add(
                        product.getCostPrice().multiply(BigDecimal.valueOf(inv.getQuantity())));
            }
        }
        result.put("inventoryValueCny", inventoryValue);

        // 毛利估算 = SUM((售价USD*汇率 - 成本CNY) * 数量) → 返回USD毛利
        BigDecimal grossProfitUsd = calcGrossProfit();
        result.put("grossProfitUsd", grossProfitUsd);

        // 毛利率：毛利 / 销售额，销售额为 0 时记 0，避免除零
        BigDecimal grossMarginRate = totalSales.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : grossProfitUsd.multiply(BigDecimal.valueOf(100)).divide(totalSales, 2, RoundingMode.HALF_UP);
        result.put("grossMarginRate", grossMarginRate);

        // 收款统计
        BigDecimal totalReceived = sumPayments(1);
        BigDecimal totalPaid = sumPayments(2);
        result.put("totalReceivedUsd", totalReceived);
        result.put("totalPaidUsd", totalPaid);

        // 待收款 = 销售总额 - 已收款
        result.put("pendingReceivableUsd", totalSales.subtract(totalReceived));

        return result;
    }

    private BigDecimal calcGrossProfit() {
        List<com.hrms.entity.OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal profit = BigDecimal.ZERO;
        for (com.hrms.entity.OrderItem item : items) {
            TradeOrder order = tradeOrderMapper.selectById(item.getOrderId());
            if (order == null || (order.getStatus() != null && order.getStatus() == 9)) {
                continue;
            }
            Product product = productMapper.selectById(item.getProductId());
            if (product == null || product.getCostPrice() == null || item.getUnitPriceUsd() == null) {
                continue;
            }
            BigDecimal rate = order.getExchangeRate() == null ? new BigDecimal("7.2") : order.getExchangeRate();
            // 收入CNY
            BigDecimal revenueCny = item.getUnitPriceUsd().multiply(rate)
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal costCny = product.getCostPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal profitCny = revenueCny.subtract(costCny);
            profit = profit.add(profitCny.divide(rate, 2, RoundingMode.HALF_UP));
        }
        return profit;
    }

    private BigDecimal sumPayments(int type) {
        QueryWrapper<com.hrms.entity.Payment> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(SUM(amount),0) AS total").eq("type", type);
        List<Map<String, Object>> maps = paymentMapper.selectMaps(wrapper);
        if (!maps.isEmpty() && maps.get(0).get("total") != null) {
            return new BigDecimal(maps.get(0).get("total").toString());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Map<String, Object> monthlySales() {
        Map<String, Object> result = new HashMap<>();
        List<String> months = new ArrayList<>();
        List<BigDecimal> sales = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();

        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthStr = month.format(fmt);
            months.add(monthStr);

            QueryWrapper<TradeOrder> wrapper = new QueryWrapper<>();
            wrapper.select("IFNULL(SUM(total_amount_usd),0) AS total", "COUNT(*) AS cnt")
                    .ne("status", 9)
                    .apply("DATE_FORMAT(order_date, '%Y-%m') = {0}", monthStr);
            List<Map<String, Object>> maps = tradeOrderMapper.selectMaps(wrapper);
            BigDecimal total = BigDecimal.ZERO;
            Long cnt = 0L;
            if (!maps.isEmpty()) {
                Map<String, Object> m = maps.get(0);
                if (m.get("total") != null) {
                    total = new BigDecimal(m.get("total").toString());
                }
                if (m.get("cnt") != null) {
                    cnt = Long.parseLong(m.get("cnt").toString());
                }
            }
            sales.add(total);
            orderCounts.add(cnt);
        }
        result.put("months", months);
        result.put("sales", sales);
        result.put("orderCounts", orderCounts);
        return result;
    }

    @Override
    public List<Map<String, Object>> topCustomers() {
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(TradeOrder::getStatus, 9);
        List<TradeOrder> orders = tradeOrderMapper.selectList(wrapper);

        Map<Long, BigDecimal> salesMap = new HashMap<>();
        Map<Long, Long> countMap = new HashMap<>();
        for (TradeOrder order : orders) {
            salesMap.merge(order.getCustomerId(),
                    order.getTotalAmountUsd() == null ? BigDecimal.ZERO : order.getTotalAmountUsd(),
                    BigDecimal::add);
            countMap.merge(order.getCustomerId(), 1L, Long::sum);
        }
        return salesMap.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> map = new HashMap<String, Object>();
                    Customer customer = customerMapper.selectById(e.getKey());
                    map.put("customerId", e.getKey());
                    map.put("customerName", customer == null ? "未知" : customer.getCompanyName());
                    map.put("country", customer == null ? "" : customer.getCountry());
                    map.put("totalSales", e.getValue());
                    map.put("orderCount", countMap.get(e.getKey()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> topProducts() {
        List<com.hrms.entity.OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, BigDecimal> salesMap = new HashMap<>();
        Map<Long, Long> qtyMap = new HashMap<>();
        for (com.hrms.entity.OrderItem item : items) {
            TradeOrder order = tradeOrderMapper.selectById(item.getOrderId());
            if (order == null || (order.getStatus() != null && order.getStatus() == 9)) {
                continue;
            }
            salesMap.merge(item.getProductId(),
                    item.getTotalPriceUsd() == null ? BigDecimal.ZERO : item.getTotalPriceUsd(),
                    BigDecimal::add);
            qtyMap.merge(item.getProductId(), (long) (item.getQuantity() == null ? 0 : item.getQuantity()), Long::sum);
        }
        return salesMap.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> map = new HashMap<String, Object>();
                    Product product = productMapper.selectById(e.getKey());
                    map.put("productId", e.getKey());
                    map.put("productName", product == null ? "未知" : product.getName());
                    map.put("sku", product == null ? "" : product.getSku());
                    map.put("totalSales", e.getValue());
                    map.put("totalQuantity", qtyMap.get(e.getKey()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> countryDistribution() {
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(TradeOrder::getStatus, 9);
        List<TradeOrder> orders = tradeOrderMapper.selectList(wrapper);

        Map<String, BigDecimal> countryMap = new HashMap<>();
        for (TradeOrder order : orders) {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            String country = customer == null || customer.getCountry() == null ? "未知" : customer.getCountry();
            countryMap.merge(country,
                    order.getTotalAmountUsd() == null ? BigDecimal.ZERO : order.getTotalAmountUsd(),
                    BigDecimal::add);
        }
        return countryMap.entrySet().stream().map(e -> {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("country", e.getKey());
            map.put("totalSales", e.getValue());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> inventoryValue() {
        List<Inventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, BigDecimal> valueMap = new HashMap<>();
        Map<Long, Long> qtyMap = new HashMap<>();
        for (Inventory inv : inventories) {
            Product product = productMapper.selectById(inv.getProductId());
            if (product != null && product.getCostPrice() != null && inv.getQuantity() != null) {
                valueMap.merge(inv.getWarehouseId(),
                        product.getCostPrice().multiply(BigDecimal.valueOf(inv.getQuantity())),
                        BigDecimal::add);
                qtyMap.merge(inv.getWarehouseId(), (long) inv.getQuantity(), Long::sum);
            }
        }
        return valueMap.entrySet().stream().map(e -> {
            Map<String, Object> map = new HashMap<String, Object>();
            com.hrms.entity.Warehouse warehouse = warehouseMapper.selectById(e.getKey());
            map.put("warehouseId", e.getKey());
            map.put("warehouseName", warehouse == null ? "未知" : warehouse.getWarehouseName());
            map.put("warehouseType", warehouse == null ? 1 : warehouse.getType());
            map.put("totalValue", e.getValue());
            map.put("totalQuantity", qtyMap.get(e.getKey()));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 毛利排行（按客户 / 按产品）
     *
     * 口径与 calcGrossProfit 保持一致：毛利 = 售价(USD) - 成本(CNY ÷ 订单汇率快照)，不含运费。
     * 用订单上存的 exchangeRate 而不是当前汇率，历史订单的毛利不会因为汇率变动被改写。
     */
    @Override
    public Map<String, Object> profitRanking() {
        List<com.hrms.entity.OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<>());

        Map<Long, BigDecimal> profitByCustomer = new HashMap<>();
        Map<Long, BigDecimal> revenueByCustomer = new HashMap<>();
        Map<Long, BigDecimal> profitByProduct = new HashMap<>();
        Map<Long, BigDecimal> revenueByProduct = new HashMap<>();

        for (com.hrms.entity.OrderItem item : items) {
            TradeOrder order = tradeOrderMapper.selectById(item.getOrderId());
            if (order == null || (order.getStatus() != null && order.getStatus() == 9)) {
                continue;
            }
            if (item.getUnitPriceUsd() == null || item.getQuantity() == null) {
                continue;
            }
            Product product = item.getProductId() == null ? null : productMapper.selectById(item.getProductId());
            BigDecimal rate = order.getExchangeRate() == null ? new BigDecimal("7.2") : order.getExchangeRate();
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());

            BigDecimal revenueUsd = item.getUnitPriceUsd().multiply(qty);
            BigDecimal costUsd = product == null || product.getCostPrice() == null
                    ? BigDecimal.ZERO
                    : product.getCostPrice().multiply(qty).divide(rate, 2, RoundingMode.HALF_UP);
            BigDecimal profitUsd = revenueUsd.subtract(costUsd);

            if (order.getCustomerId() != null) {
                profitByCustomer.merge(order.getCustomerId(), profitUsd, BigDecimal::add);
                revenueByCustomer.merge(order.getCustomerId(), revenueUsd, BigDecimal::add);
            }
            if (item.getProductId() != null) {
                profitByProduct.merge(item.getProductId(), profitUsd, BigDecimal::add);
                revenueByProduct.merge(item.getProductId(), revenueUsd, BigDecimal::add);
            }
        }

        List<Map<String, Object>> byCustomer = profitByCustomer.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Customer customer = customerMapper.selectById(e.getKey());
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("id", e.getKey());
                    map.put("name", customer == null ? "未知客户" : customer.getCompanyName());
                    map.put("country", customer == null ? "" : customer.getCountry());
                    map.put("profitUsd", e.getValue());
                    map.put("revenueUsd", revenueByCustomer.get(e.getKey()));
                    map.put("marginRate", marginRate(e.getValue(), revenueByCustomer.get(e.getKey())));
                    return map;
                }).collect(Collectors.toList());

        List<Map<String, Object>> byProduct = profitByProduct.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Product product = productMapper.selectById(e.getKey());
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("id", e.getKey());
                    map.put("name", product == null ? "未知产品" : product.getName());
                    map.put("sku", product == null ? "" : product.getSku());
                    map.put("profitUsd", e.getValue());
                    map.put("revenueUsd", revenueByProduct.get(e.getKey()));
                    map.put("marginRate", marginRate(e.getValue(), revenueByProduct.get(e.getKey())));
                    return map;
                }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("byCustomer", byCustomer);
        result.put("byProduct", byProduct);
        return result;
    }

    /** 毛利率（%），收入为 0 或空时记 0 */
    private BigDecimal marginRate(BigDecimal profit, BigDecimal revenue) {
        if (profit == null || revenue == null || revenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP);
    }
}
