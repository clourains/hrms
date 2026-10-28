package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.dto.ImportResult;
import com.hrms.dto.OrderCreateDTO;
import com.hrms.entity.Customer;
import com.hrms.entity.Invoice;
import com.hrms.entity.OfflineDeal;
import com.hrms.entity.OrderItem;
import com.hrms.entity.Product;
import com.hrms.entity.Shipment;
import com.hrms.entity.ShipmentTrack;
import com.hrms.entity.Supplier;
import com.hrms.entity.TradeOrder;
import com.hrms.entity.Warehouse;
import com.hrms.mapper.CustomerMapper;
import com.hrms.mapper.InvoiceMapper;
import com.hrms.mapper.OfflineDealMapper;
import com.hrms.mapper.ProductMapper;
import com.hrms.mapper.ShipmentMapper;
import com.hrms.mapper.ShipmentTrackMapper;
import com.hrms.mapper.SupplierMapper;
import com.hrms.mapper.TradeOrderMapper;
import com.hrms.mapper.WarehouseMapper;
import com.hrms.service.CustomerService;
import com.hrms.service.DataImportService;
import com.hrms.service.InvoiceService;
import com.hrms.service.OfflineDealService;
import com.hrms.service.ProductService;
import com.hrms.service.SupplierService;
import com.hrms.service.TradeOrderService;
import com.hrms.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务数据批量导入实现
 *
 * 判重口径：有唯一编码的数据（客户/供应商/SKU/订单号/发票号/交易编号）一律
 * 「存在就整行跳过」，并在结果里说明原因；不做静默覆盖，避免导入把人工维护的数据冲掉。
 */
@Service
public class DataImportServiceImpl implements DataImportService {

    private static final Map<String, String> MODULE_LABELS = new LinkedHashMap<>();
    private static final Map<String, List<String>> MODULE_HEADERS = new LinkedHashMap<>();
    private static final Map<String, List<String>> MODULE_SAMPLES = new LinkedHashMap<>();

    static {
        MODULE_LABELS.put("customer", "客户");
        MODULE_LABELS.put("supplier", "供应商");
        MODULE_LABELS.put("product", "产品");
        MODULE_LABELS.put("order", "销售订单");
        MODULE_LABELS.put("invoice", "发票");
        MODULE_LABELS.put("offline-deal", "线下交易");
        MODULE_LABELS.put("shipment", "发货单（运单）");

        MODULE_HEADERS.put("customer", Arrays.asList(
                "客户编码", "公司名称", "联系人", "国家", "城市", "地址",
                "电话", "邮箱", "网站", "等级", "信用额度(USD)", "备注"));
        MODULE_SAMPLES.put("customer", Arrays.asList(
                "C-IMP-001", "示例贸易有限公司", "张三", "美国", "纽约", "123 Main St",
                "+1 212-000-0000", "sales@example.com", "https://example.com", "A", "5000", "Excel 导入示例"));

        MODULE_HEADERS.put("supplier", Arrays.asList(
                "供应商编码", "公司名称", "联系人", "国家", "城市", "电话", "邮箱", "供应类别", "备注"));
        MODULE_SAMPLES.put("supplier", Arrays.asList(
                "S-IMP-001", "示例供应链有限公司", "李四", "中国", "深圳", "0755-00000000",
                "supply@example.com", "电子元件", "Excel 导入示例"));

        MODULE_HEADERS.put("product", Arrays.asList(
                "SKU", "产品名称", "英文名", "类别", "单位", "成本价", "销售价(USD)", "描述", "状态"));
        MODULE_SAMPLES.put("product", Arrays.asList(
                "SKU-IMP-001", "便携储能电源", "Portable Power Station", "电子产品", "pcs",
                "150.00", "299.00", "1000Wh 户外电源", "1"));

        MODULE_HEADERS.put("order", Arrays.asList(
                "外部单号(订单号)", "客户编码", "订单日期", "产品SKU", "数量", "单价(USD)", "仓库编码", "备注"));
        MODULE_SAMPLES.put("order", Arrays.asList(
                "SO-IMP-001", "CUS-CA-001", "2026-10-20", "SKU-ELE-001", "100", "299.00", "WH-CN-SZ",
                "同一外部单号的多行会合并为一张订单"));

        MODULE_HEADERS.put("invoice", Arrays.asList(
                "订单号", "发票号", "抬头", "税号", "发票类型", "币种", "金额", "备注"));
        MODULE_SAMPLES.put("invoice", Arrays.asList(
                "SO-IMP-001", "INV-IMP-001", "示例贸易有限公司", "TAX123456", "普通发票", "USD", "29900",
                "发票号可留空，系统自动生成"));

        MODULE_HEADERS.put("offline-deal", Arrays.asList(
                "交易编号", "客户名称", "国家", "商品/服务", "币种", "金额", "成交日期", "收款方式", "凭证", "备注"));
        MODULE_SAMPLES.put("offline-deal", Arrays.asList(
                "OD-IMP-001", "示例贸易有限公司", "美国", "样品采购", "USD", "1500", "2026-10-20",
                "银行转账", "VOUCHER-001", "交易编号可留空，系统自动生成"));

        MODULE_HEADERS.put("shipment", Arrays.asList(
                "运单号", "承运商", "运输方式", "起运港", "目的港", "发货日期",
                "关联订单号", "预计到港", "运费(USD)", "备注"));
        MODULE_SAMPLES.put("shipment", Arrays.asList(
                "1Z999AA10123456784", "UPS", "快递", "上海港", "美国纽约", "2026-10-20",
                "SO-202605-001", "2026-10-28", "0", "导入后可在物流跟踪里同步 17TRACK 轨迹"));
    }

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ProductService productService;

    @Autowired
    private TradeOrderService tradeOrderService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private OfflineDealService offlineDealService;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Autowired
    private OfflineDealMapper offlineDealMapper;

    @Autowired
    private ShipmentMapper shipmentMapper;

    @Autowired
    private ShipmentTrackMapper shipmentTrackMapper;

    @Override
    public String moduleLabel(String module) {
        String label = MODULE_LABELS.get(module);
        if (label == null) {
            throw new RuntimeException("不支持的导入模块：" + module);
        }
        return label;
    }

    @Override
    public List<String> headers(String module) {
        moduleLabel(module);
        return MODULE_HEADERS.get(module);
    }

    @Override
    public List<String> sampleRow(String module) {
        moduleLabel(module);
        return MODULE_SAMPLES.get(module);
    }

    @Override
    public ImportResult importData(String module, List<Map<String, String>> rows) {
        moduleLabel(module);
        if (rows == null || rows.isEmpty()) {
            throw new RuntimeException("没有可导入的数据");
        }
        ImportResult result = new ImportResult();
        result.setTotal(rows.size());

        switch (module) {
            case "customer" -> rows.forEach(row -> importCustomer(row, result));
            case "supplier" -> rows.forEach(row -> importSupplier(row, result));
            case "product" -> rows.forEach(row -> importProduct(row, result));
            case "order" -> importOrders(rows, result);
            case "invoice" -> rows.forEach(row -> importInvoice(row, result));
            case "offline-deal" -> rows.forEach(row -> importOfflineDeal(row, result));
            case "shipment" -> rows.forEach(row -> importShipment(row, result));
            default -> throw new RuntimeException("不支持的导入模块：" + module);
        }
        return result;
    }

    /* ================= 客户 ================= */

    private void importCustomer(Map<String, String> row, ImportResult result) {
        int rowNo = rowNo(row);
        String code = text(row, "客户编码");
        if (StringUtils.hasText(code) && customerMapper.countByCodeIncludeDeleted(code) > 0) {
            result.addSkipped(1, rowNo, "客户编码「" + code + "」已存在，已跳过");
            return;
        }
        try {
            Customer customer = new Customer();
            customer.setCustomerCode(code);
            customer.setCompanyName(text(row, "公司名称"));
            customer.setContactName(text(row, "联系人"));
            customer.setCountry(text(row, "国家"));
            customer.setCity(text(row, "城市"));
            customer.setAddress(text(row, "地址"));
            customer.setPhone(text(row, "电话"));
            customer.setEmail(text(row, "邮箱"));
            customer.setWebsite(text(row, "网站"));
            customer.setLevel(text(row, "等级"));
            customer.setCreditLimitUsd(decimal(row, "信用额度(USD)"));
            customer.setRemark(text(row, "备注"));
            customerService.save(customer);
            result.setSuccess(result.getSuccess() + 1);
        } catch (Exception e) {
            result.addError(rowNo, message(e));
        }
    }

    /* ================= 供应商 ================= */

    private void importSupplier(Map<String, String> row, ImportResult result) {
        int rowNo = rowNo(row);
        String code = text(row, "供应商编码");
        if (StringUtils.hasText(code) && supplierMapper.countByCodeIncludeDeleted(code) > 0) {
            result.addSkipped(1, rowNo, "供应商编码「" + code + "」已存在，已跳过");
            return;
        }
        try {
            Supplier supplier = new Supplier();
            supplier.setSupplierCode(code);
            supplier.setCompanyName(text(row, "公司名称"));
            supplier.setContactName(text(row, "联系人"));
            supplier.setCountry(text(row, "国家"));
            supplier.setCity(text(row, "城市"));
            supplier.setPhone(text(row, "电话"));
            supplier.setEmail(text(row, "邮箱"));
            supplier.setCategory(text(row, "供应类别"));
            supplier.setRemark(text(row, "备注"));
            supplierService.save(supplier);
            result.setSuccess(result.getSuccess() + 1);
        } catch (Exception e) {
            result.addError(rowNo, message(e));
        }
    }

    /* ================= 产品 ================= */

    private void importProduct(Map<String, String> row, ImportResult result) {
        int rowNo = rowNo(row);
        String sku = text(row, "SKU");
        if (StringUtils.hasText(sku) && productMapper.countBySkuIncludeDeleted(sku) > 0) {
            result.addSkipped(1, rowNo, "SKU「" + sku + "」已存在，已跳过");
            return;
        }
        try {
            Product product = new Product();
            product.setSku(sku);
            product.setName(text(row, "产品名称"));
            product.setNameEn(text(row, "英文名"));
            product.setCategory(text(row, "类别"));
            product.setUnit(text(row, "单位"));
            product.setCostPrice(decimal(row, "成本价"));
            product.setSalePriceUsd(decimal(row, "销售价(USD)"));
            product.setDescription(text(row, "描述"));
            product.setStatus(statusOf(text(row, "状态"), 1));
            productService.save(product);
            result.setSuccess(result.getSuccess() + 1);
        } catch (Exception e) {
            result.addError(rowNo, message(e));
        }
    }

    /* ================= 销售订单（同一外部单号的多行合并为一张单） ================= */

    private void importOrders(List<Map<String, String>> rows, ImportResult result) {
        Map<String, List<Map<String, String>>> groups = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String orderNo = text(row, "外部单号(订单号)");
            if (!StringUtils.hasText(orderNo)) {
                result.addError(rowNo(row), "外部单号(订单号)不能为空");
                continue;
            }
            groups.computeIfAbsent(orderNo, k -> new ArrayList<>()).add(row);
        }
        groups.forEach((orderNo, groupRows) -> importOrderGroup(orderNo, groupRows, result));
    }

    private void importOrderGroup(String orderNo, List<Map<String, String>> rows, ImportResult result) {
        int firstRow = rowNo(rows.get(0));
        if (tradeOrderMapper.countByNoIncludeDeleted(orderNo) > 0) {
            result.addSkipped(rows.size(), firstRow, "订单号「" + orderNo + "」已存在，整单跳过");
            return;
        }

        String customerCode = text(rows.get(0), "客户编码");
        Customer customer = customerCode.isEmpty() ? null
                : customerMapper.selectOne(new LambdaQueryWrapper<Customer>().eq(Customer::getCustomerCode, customerCode).last("LIMIT 1"));
        if (customer == null) {
            result.addError(firstRow, "客户编码「" + customerCode + "」不存在，请先导入或新建客户");
            return;
        }

        LocalDate orderDate;
        try {
            orderDate = date(rows.get(0), "订单日期");
        } catch (Exception e) {
            result.addError(firstRow, message(e));
            return;
        }
        if (orderDate == null) {
            result.addError(firstRow, "订单日期不能为空");
            return;
        }

        List<OrderItem> items = new ArrayList<>();
        List<String> lineErrors = new ArrayList<>();
        for (Map<String, String> row : rows) {
            int rowNo = rowNo(row);
            String sku = text(row, "产品SKU");
            Product product = sku.isEmpty() ? null
                    : productMapper.selectOne(new LambdaQueryWrapper<Product>().eq(Product::getSku, sku).last("LIMIT 1"));
            if (product == null) {
                lineErrors.add("第 " + rowNo + " 行：产品SKU「" + sku + "」不存在");
                continue;
            }
            String warehouseCode = text(row, "仓库编码");
            Warehouse warehouse = warehouseCode.isEmpty() ? null
                    : warehouseMapper.selectOne(new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getWarehouseCode, warehouseCode).last("LIMIT 1"));
            if (warehouse == null) {
                lineErrors.add("第 " + rowNo + " 行：仓库编码「" + warehouseCode + "」不存在");
                continue;
            }
            try {
                Integer quantity = integer(row, "数量");
                if (quantity == null || quantity <= 0) {
                    lineErrors.add("第 " + rowNo + " 行：数量必须大于 0");
                    continue;
                }
                OrderItem item = new OrderItem();
                item.setProductId(product.getId());
                item.setWarehouseId(warehouse.getId());
                item.setQuantity(quantity);
                item.setUnitPriceUsd(decimal(row, "单价(USD)"));
                items.add(item);
            } catch (Exception e) {
                lineErrors.add("第 " + rowNo + " 行：" + message(e));
            }
        }

        if (!lineErrors.isEmpty()) {
            result.addError(firstRow, String.join("；", lineErrors));
            return;
        }
        if (items.isEmpty()) {
            result.addError(firstRow, "没有有效的订单明细");
            return;
        }

        try {
            TradeOrder order = new TradeOrder();
            order.setOrderNo(orderNo);
            order.setCustomerId(customer.getId());
            order.setOrderDate(orderDate);
            order.setRemark(text(rows.get(0), "备注"));
            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setOrder(order);
            dto.setItems(items);
            tradeOrderService.createOrder(dto);
            result.setSuccess(result.getSuccess() + rows.size());
        } catch (Exception e) {
            result.addError(firstRow, message(e));
        }
    }

    /* ================= 发票 ================= */

    private void importInvoice(Map<String, String> row, ImportResult result) {
        int rowNo = rowNo(row);
        String orderNo = text(row, "订单号");
        if (!StringUtils.hasText(orderNo)) {
            result.addError(rowNo, "订单号不能为空");
            return;
        }
        TradeOrder order = tradeOrderMapper.selectOne(
                new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getOrderNo, orderNo).last("LIMIT 1"));
        if (order == null) {
            result.addError(rowNo, "订单号「" + orderNo + "」不存在");
            return;
        }
        String invoiceNo = text(row, "发票号");
        if (StringUtils.hasText(invoiceNo) && invoiceMapper.countByNoIncludeDeleted(invoiceNo) > 0) {
            result.addSkipped(1, rowNo, "发票号「" + invoiceNo + "」已存在，已跳过");
            return;
        }
        try {
            Invoice invoice = new Invoice();
            invoice.setOrderId(order.getId());
            invoice.setInvoiceNo(invoiceNo);
            invoice.setTitle(text(row, "抬头"));
            invoice.setTaxNo(text(row, "税号"));
            invoice.setInvoiceType(StringUtils.hasText(text(row, "发票类型")) ? text(row, "发票类型") : "普通发票");
            invoice.setCurrency(StringUtils.hasText(text(row, "币种")) ? text(row, "币种").toUpperCase() : order.getCurrency());
            invoice.setAmount(decimal(row, "金额"));
            invoice.setRemark(text(row, "备注"));
            invoiceService.createFromOrder(invoice);
            result.setSuccess(result.getSuccess() + 1);
        } catch (Exception e) {
            result.addError(rowNo, message(e));
        }
    }

    /* ================= 线下交易 ================= */

    private void importOfflineDeal(Map<String, String> row, ImportResult result) {
        int rowNo = rowNo(row);
        String dealNo = text(row, "交易编号");
        if (StringUtils.hasText(dealNo) && offlineDealMapper.countByNoIncludeDeleted(dealNo) > 0) {
            result.addSkipped(1, rowNo, "交易编号「" + dealNo + "」已存在，已跳过");
            return;
        }
        try {
            OfflineDeal deal = new OfflineDeal();
            deal.setDealNo(dealNo);
            deal.setCustomerName(text(row, "客户名称"));
            deal.setCustomerCountry(text(row, "国家"));
            deal.setItemName(text(row, "商品/服务"));
            deal.setCurrency(StringUtils.hasText(text(row, "币种")) ? text(row, "币种").toUpperCase() : "USD");
            deal.setAmount(decimal(row, "金额"));
            deal.setDealDate(date(row, "成交日期"));
            deal.setPayMethod(payMethod(text(row, "收款方式")));
            deal.setVoucher(text(row, "凭证"));
            deal.setRemark(text(row, "备注"));
            offlineDealService.save(deal);
            result.setSuccess(result.getSuccess() + 1);
        } catch (Exception e) {
            result.addError(rowNo, message(e));
        }
    }

    /* ================= 发货单（17TRACK 运单） ================= */

    /**
     * 运单批量导入：有运单号即建发货单并进入「运输中」，同时落一条「已交运」节点，
     * 之后由自动同步任务或手动同步补全 17TRACK 详细轨迹。运单号重复直接跳过。
     */
    private void importShipment(Map<String, String> row, ImportResult result) {
        int rowNo = rowNo(row);
        String trackingNo = text(row, "运单号");
        if (!StringUtils.hasText(trackingNo)) {
            result.addError(rowNo, "运单号不能为空");
            return;
        }
        if (shipmentMapper.selectCount(new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getTrackingNo, trackingNo)) > 0) {
            result.addSkipped(1, rowNo, "运单号「" + trackingNo + "」已存在，已跳过");
            return;
        }
        try {
            Shipment shipment = new Shipment();
            shipment.setTrackingNo(trackingNo);
            shipment.setCarrier(text(row, "承运商"));
            shipment.setTransportType(StringUtils.hasText(text(row, "运输方式")) ? text(row, "运输方式") : "快递");
            shipment.setPortFrom(text(row, "起运港"));
            shipment.setPortTo(text(row, "目的港"));
            LocalDate shipDate = date(row, "发货日期");
            shipment.setShipDate(shipDate != null ? shipDate : LocalDate.now());
            shipment.setEta(date(row, "预计到港"));
            shipment.setFreightCostUsd(decimal(row, "运费(USD)"));
            shipment.setRemark(text(row, "备注"));

            String orderNo = text(row, "关联订单号");
            if (StringUtils.hasText(orderNo)) {
                TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getOrderNo, orderNo).last("LIMIT 1"));
                if (order == null) {
                    result.addError(rowNo, "订单号「" + orderNo + "」不存在，可清空该列后重新导入");
                    return;
                }
                shipment.setOrderId(order.getId());
            }

            shipment.setStatus(2);
            shipment.setShipmentNo(generateImportShipmentNo(rowNo));
            shipment.setCreateTime(LocalDateTime.now());
            shipment.setUpdateTime(LocalDateTime.now());
            shipmentMapper.insert(shipment);

            ShipmentTrack track = new ShipmentTrack();
            track.setShipmentId(shipment.getId());
            track.setStatus(2);
            track.setNode("已交运");
            track.setLocation(shipment.getPortFrom());
            track.setContent("运单导入，等待同步 17TRACK 轨迹");
            track.setTrackTime(LocalDateTime.now());
            track.setOperator("Excel导入");
            track.setCreateTime(LocalDateTime.now());
            shipmentTrackMapper.insert(track);

            result.setSuccess(result.getSuccess() + 1);
        } catch (Exception e) {
            result.addError(rowNo, message(e));
        }
    }

    private String generateImportShipmentNo(int rowNo) {
        String base = "SH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String no = rowNo > 0 ? base + "-" + rowNo : base;
        if (shipmentMapper.countByNoIncludeDeleted(no) > 0) {
            no = no + "-" + (System.nanoTime() % 1000);
        }
        return no;
    }

    /* ================= 解析辅助 ================= */

    private int rowNo(Map<String, String> row) {
        String value = row.get(ExcelUtil.ROW_KEY);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String text(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null ? "" : value.trim();
    }

    private BigDecimal decimal(Map<String, String> row, String key) {
        String value = text(row, key).replace(",", "").replace("$", "").replace("¥", "").replace("€", "");
        if (value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException(key + "「" + value + "」不是有效数字");
        }
    }

    private Integer integer(Map<String, String> row, String key) {
        String value = text(row, key).replace(",", "");
        if (value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value).intValue();
        } catch (NumberFormatException e) {
            throw new RuntimeException(key + "「" + value + "」不是有效整数");
        }
    }

    private LocalDate date(Map<String, String> row, String key) {
        String value = text(row, key);
        if (value.isEmpty()) {
            return null;
        }
        String normalized = value.replace("/", "-");
        int space = normalized.indexOf(' ');
        if (space > 0) {
            normalized = normalized.substring(0, space);
        }
        String[] parts = normalized.split("-");
        if (parts.length == 3) {
            try {
                return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (Exception ignored) {
                // 落到下面的统一报错
            }
        }
        throw new RuntimeException(key + "「" + value + "」格式应为 yyyy-MM-dd");
    }

    /**
     * 状态列：支持中文/数字。上架=1、下架=0
     */
    private int statusOf(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        String v = value.trim();
        if ("上架".equals(v) || "启用".equals(v) || "1".equals(v)) {
            return 1;
        }
        if ("下架".equals(v) || "停用".equals(v) || "0".equals(v)) {
            return 0;
        }
        return defaultValue;
    }

    /**
     * 收款方式：支持中文/数字，无法识别按「其他」
     */
    private int payMethod(String value) {
        if (!StringUtils.hasText(value)) {
            return 1;
        }
        String v = value.trim();
        switch (v) {
            case "银行转账" -> {
                return 1;
            }
            case "现金" -> {
                return 2;
            }
            case "微信" -> {
                return 3;
            }
            case "支付宝" -> {
                return 4;
            }
            case "其他" -> {
                return 9;
            }
            default -> {
                // 兼容直接传数字
            }
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 9;
        }
    }

    private String message(Exception e) {
        String msg = e.getMessage();
        return StringUtils.hasText(msg) ? msg : "数据格式不正确";
    }
}
