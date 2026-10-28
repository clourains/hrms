package com.hrms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务侧智能分析 DTO
 *
 * 业务数据（预测 / 客户 / 库存 / 物流）始终由规则基于真实业务表计算；
 * 洞察文字与预测解读可由大模型生成，失败时回退为规则结果。
 */
public class BusinessInsightDTO {

    /**
     * 业务智能洞察总览
     */
    @Data
    public static class BusinessInsights {
        private String source = "rule";                     // rule=规则推算 / model=AI 生成
        private boolean aiEnabled;                           // 是否已配置可用的 AI Key
        private String model;                                // 调用模型时填入模型名，规则为空
        private long elapsedMs;                              // 本次请求耗时（缓存回放为 0）
        private String generatedAt;                          // 生成时间 yyyy-MM-dd HH:mm:ss
        private String aiError;                              // 回退规则时的模型失败原因（成功时为空）
        private SalesForecast forecast = new SalesForecast();
        private List<CustomerRisk> customerRisk = new ArrayList<>();
        private List<RestockSuggestion> restock = new ArrayList<>();
        private List<LogisticsAlert> logisticsAlerts = new ArrayList<>();
        private List<Insight> insights = new ArrayList<>();
    }

    /**
     * 销售预测：近 12 个月销售额线性回归，外推未来 3 个月
     */
    @Data
    public static class SalesForecast {
        private List<String> months = new ArrayList<>();     // 未来 3 个月 yyyy-MM
        private List<Double> values = new ArrayList<>();     // 预测销售额（USD）
        private Double growthRate = 0.0;                     // 未来 3 个月均值 / 最近 3 个月均值 - 1
        private Integer sampleMonths = 0;                    // 参与回归的月份数
        private String method = "近12个月线性回归";
        private String comment;                              // 模型对预测的一句话解读（规则版为空）
    }

    /**
     * 客户流失预警
     */
    @Data
    public static class CustomerRisk {
        private String customerName;
        private String country;
        private String lastOrderDate;                        // yyyy-MM-dd
        private Integer daysSince;                           // 距最后下单天数
        private Integer orderCount;                          // 历史有效订单数
        private BigDecimal totalSalesUsd;                    // 历史销售额（USD）
        private String level;                                // high / medium
        private String reason;
    }

    /**
     * 库存补货建议
     */
    @Data
    public static class RestockSuggestion {
        private String productName;
        private String sku;
        private String warehouse;
        private Integer stock;                               // 当前库存数量
        private Double monthlySales;                         // 近 90 天折合月均销量
        private Integer daysOfSupply;                        // 可售天数（库存 ÷ 日均销量）
        private Integer suggestedQty;                        // 建议补货量
        private String level;                                // high / medium
    }

    /**
     * 物流异常
     */
    @Data
    public static class LogisticsAlert {
        private String shipmentNo;
        private String trackingNo;
        private String carrier;
        private Integer status;                              // 2 运输中 / 3 清关中
        private String eta;                                  // yyyy-MM-dd
        private Integer daysOverdue;                         // 超过 ETA 天数
        private Integer daysSinceTrack;                      // 最新轨迹距今天数
        private String issue;
    }

    /**
     * 规则生成的洞察条目
     */
    @Data
    public static class Insight {
        private String title;
        private String type;                                 // warning / success / info
        private String category;                             // sales / customer / stock / logistics
        private String description;
        private String suggestion;
    }
}
