package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.service.OpenApiKeyService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 开放接口密钥管理（仅管理员）
 */
@RequireRole("admin")
@RestController
@RequestMapping("/open-api")
public class OpenApiAdminController {

    @Autowired
    private OpenApiKeyService openApiKeyService;

    /**
     * 查看当前密钥与可调用的批量写入接口
     */
    @GetMapping("/credential")
    public Result<Map<String, Object>> credential() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("apiKey", openApiKeyService.getOrCreate());
        data.put("header", "X-API-Key");
        data.put("endpoints", endpoints());
        return Result.success(data);
    }

    /**
     * 重置密钥，旧密钥立即失效
     */
    @PostMapping("/credential/rotate")
    public Result<Map<String, Object>> rotate() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("apiKey", openApiKeyService.rotate());
        return Result.success("密钥已重置", data);
    }

    /**
     * 接口清单（前端「开放接口」弹窗直接渲染）
     */
    private List<Map<String, String>> endpoints() {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(endpoint("客户批量写入", "/open-api/import/customer",
                "customerCode/companyName/contactName/country/city/address/phone/email/website/level/creditLimitUsd/remark"));
        list.add(endpoint("供应商批量写入", "/open-api/import/supplier",
                "supplierCode/companyName/contactName/country/city/phone/email/category/remark"));
        list.add(endpoint("产品批量写入", "/open-api/import/product",
                "sku/name/nameEn/category/unit/costPrice/salePriceUsd/description/status"));
        list.add(endpoint("销售订单批量写入", "/open-api/import/order",
                "orderNo/customerCode/orderDate/remark + items[sku/quantity/unitPriceUsd/warehouseCode]"));
        list.add(endpoint("发票批量写入", "/open-api/import/invoice",
                "orderNo/invoiceNo/title/taxNo/invoiceType/currency/amount/remark"));
        list.add(endpoint("线下交易批量写入", "/open-api/import/offline-deal",
                "dealNo/customerName/customerCountry/itemName/currency/amount/dealDate/payMethod/voucher/remark"));
        return list;
    }

    private Map<String, String> endpoint(String name, String path, String fields) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("method", "POST");
        item.put("path", path);
        item.put("fields", fields);
        return item;
    }
}
