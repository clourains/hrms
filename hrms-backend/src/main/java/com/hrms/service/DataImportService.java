package com.hrms.service;

import com.hrms.dto.ImportResult;

import java.util.List;
import java.util.Map;

/**
 * 业务数据批量导入（Excel 上传与开放 API 共用同一套校验逻辑）
 */
public interface DataImportService {

    /** 支持的导入模块 */
    String[] MODULES = {"customer", "supplier", "product", "order", "invoice", "offline-deal"};

    /**
     * 按模块导入行数据，Excel 与 API 最终都走这里
     *
     * @param module customer/supplier/product/order/invoice/offline-deal
     * @param rows   每行一个「表头名 → 文本值」的 Map，可选 "__row" 记录 Excel 行号
     */
    ImportResult importData(String module, List<Map<String, String>> rows);

    /** 模板表头 */
    List<String> headers(String module);

    /** 模板示例行 */
    List<String> sampleRow(String module);

    /** 模块中文名（用于提示与文件名） */
    String moduleLabel(String module);
}
