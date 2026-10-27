package com.hrms.service;

import java.util.List;
import java.util.Map;

/**
 * 营业分析Service接口
 */
public interface BusinessService {

    /**
     * 经营总览卡片：销售额/订单量/客户数/库存总值/毛利/收款
     */
    Map<String, Object> overview();

    /**
     * 近12个月销售额与订单量
     */
    Map<String, Object> monthlySales();

    /**
     * TOP5 客户（按销售额）
     */
    List<Map<String, Object>> topCustomers();

    /**
     * TOP5 产品（按销售额）
     */
    List<Map<String, Object>> topProducts();

    /**
     * 订单国家分布
     */
    List<Map<String, Object>> countryDistribution();

    /**
     * 分仓库库存价值
     */
    List<Map<String, Object>> inventoryValue();

    /**
     * 毛利排行：按客户、按产品各取 TOP10，附毛利率
     */
    Map<String, Object> profitRanking();
}
