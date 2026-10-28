package com.hrms.controller;

import com.hrms.dto.ImportResult;
import com.hrms.service.DataImportService;
import com.hrms.service.ImportAuditService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 开放接口：外部系统/脚本批量写入业务数据
 *
 * 鉴权在 JwtAuthenticationFilter 里完成（请求头 X-API-Key），
 * 校验逻辑与 Excel 导入共用 DataImportService，保证两边口径一致。
 *
 * 请求体：{"items": [ {...}, {...} ]}
 * 返回：成功 / 跳过 / 失败 数量及逐行原因
 */
@RestController
@RequestMapping("/open-api/import")
public class OpenApiImportController {

    /** 英文请求字段 → Excel 表头（同时兼容直接传中文表头） */
    private static final Map<String, Map<String, String>> ALIASES = new LinkedHashMap<>();

    static {
        Map<String, String> customer = new LinkedHashMap<>();
        customer.put("customerCode", "客户编码");
        customer.put("companyName", "公司名称");
        customer.put("contactName", "联系人");
        customer.put("country", "国家");
        customer.put("city", "城市");
        customer.put("address", "地址");
        customer.put("phone", "电话");
        customer.put("email", "邮箱");
        customer.put("website", "网站");
        customer.put("level", "等级");
        customer.put("creditLimitUsd", "信用额度(USD)");
        customer.put("remark", "备注");
        ALIASES.put("customer", customer);

        Map<String, String> supplier = new LinkedHashMap<>();
        supplier.put("supplierCode", "供应商编码");
        supplier.put("companyName", "公司名称");
        supplier.put("contactName", "联系人");
        supplier.put("country", "国家");
        supplier.put("city", "城市");
        supplier.put("phone", "电话");
        supplier.put("email", "邮箱");
        supplier.put("category", "供应类别");
        supplier.put("remark", "备注");
        ALIASES.put("supplier", supplier);

        Map<String, String> product = new LinkedHashMap<>();
        product.put("sku", "SKU");
        product.put("name", "产品名称");
        product.put("nameEn", "英文名");
        product.put("category", "类别");
        product.put("unit", "单位");
        product.put("costPrice", "成本价");
        product.put("salePriceUsd", "销售价(USD)");
        product.put("description", "描述");
        product.put("status", "状态");
        ALIASES.put("product", product);

        Map<String, String> invoice = new LinkedHashMap<>();
        invoice.put("orderNo", "订单号");
        invoice.put("invoiceNo", "发票号");
        invoice.put("title", "抬头");
        invoice.put("taxNo", "税号");
        invoice.put("invoiceType", "发票类型");
        invoice.put("currency", "币种");
        invoice.put("amount", "金额");
        invoice.put("remark", "备注");
        ALIASES.put("invoice", invoice);

        Map<String, String> offline = new LinkedHashMap<>();
        offline.put("dealNo", "交易编号");
        offline.put("customerName", "客户名称");
        offline.put("customerCountry", "国家");
        offline.put("itemName", "商品/服务");
        offline.put("currency", "币种");
        offline.put("amount", "金额");
        offline.put("dealDate", "成交日期");
        offline.put("payMethod", "收款方式");
        offline.put("voucher", "凭证");
        offline.put("remark", "备注");
        ALIASES.put("offline-deal", offline);

        // 运单：外部系统（如 17TRACK 对接方）可直接推运单号建发货单
        Map<String, String> shipment = new LinkedHashMap<>();
        shipment.put("trackingNo", "运单号");
        shipment.put("carrier", "承运商");
        shipment.put("transportType", "运输方式");
        shipment.put("portFrom", "起运港");
        shipment.put("portTo", "目的港");
        shipment.put("shipDate", "发货日期");
        shipment.put("orderNo", "关联订单号");
        shipment.put("eta", "预计到港");
        shipment.put("freightCostUsd", "运费(USD)");
        shipment.put("remark", "备注");
        ALIASES.put("shipment", shipment);
    }

    @Autowired
    private DataImportService dataImportService;

    @Autowired
    private ImportAuditService importAuditService;

    @PostMapping("/{module}")
    public Result<ImportResult> importData(@PathVariable String module,
                                           @RequestBody Map<String, Object> body) {
        dataImportService.moduleLabel(module);
        Object items = body == null ? null : body.get("items");
        if (!(items instanceof List<?> list) || list.isEmpty()) {
            throw new RuntimeException("请求体需包含非空的 items 数组，例如 {\"items\":[{...}]}");
        }
        ImportResult result = dataImportService.importData(module, toRows(module, list));
        // 留痕：外部写入同样记录来源与数量，便于事后审计
        importAuditService.record(module, "API", null, result);
        return Result.success(result);
    }

    /**
     * 把接口 JSON 转成导入服务认的「表头 → 文本」行
     */
    private List<Map<String, String>> toRows(String module, List<?> items) {
        List<Map<String, String>> rows = new ArrayList<>();
        if ("order".equals(module)) {
            // 订单是一张单带多条明细：把单头字段复制到每条明细行
            for (Object element : items) {
                if (!(element instanceof Map<?, ?> order)) {
                    throw new RuntimeException("items 里的订单必须是对象");
                }
                String orderNo = str(order, "orderNo");
                String customerCode = str(order, "customerCode");
                String orderDate = str(order, "orderDate");
                String remark = str(order, "remark");
                Object lines = order.get("items");
                if (!(lines instanceof List<?> lineList)) {
                    throw new RuntimeException("订单「" + orderNo + "」缺少 items 明细数组");
                }
                for (Object lineElement : lineList) {
                    if (!(lineElement instanceof Map<?, ?> line)) {
                        throw new RuntimeException("订单明细必须是对象");
                    }
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("外部单号(订单号)", orderNo);
                    row.put("客户编码", customerCode);
                    row.put("订单日期", orderDate);
                    row.put("备注", remark);
                    row.put("产品SKU", str(line, "sku"));
                    row.put("数量", str(line, "quantity"));
                    row.put("单价(USD)", str(line, "unitPriceUsd"));
                    row.put("仓库编码", str(line, "warehouseCode"));
                    rows.add(row);
                }
            }
            return rows;
        }

        Map<String, String> aliases = ALIASES.get(module);
        for (Object element : items) {
            if (!(element instanceof Map<?, ?> item)) {
                throw new RuntimeException("items 里的每个元素必须是对象");
            }
            Map<String, String> row = new LinkedHashMap<>();
            aliases.forEach((field, header) -> {
                // 优先取英文字段，其次允许直接传中文表头
                String value = item.containsKey(field) ? str(item, field) : str(item, header);
                if (value != null) {
                    row.put(header, value);
                }
            });
            rows.add(row);
        }
        return rows;
    }

    private String str(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
