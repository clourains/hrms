package com.hrms.service;

import java.util.Map;

/**
 * 智能洞察服务
 *
 * 页面默认展示最近一次 AI 分析结果（不再默认走规则推算）：
 * 1. generate=false：回放已留存的结果，附带生成时间距今多久、是否过期；没有结果时自动生成一次；
 * 2. generate=true：用户点「AI 深度分析」，强制重新调用模型并覆盖结果；
 * 3. 模型不可用时：有历史结果优先展示历史结果，没有再回落规则推算并明确标注。
 */
public interface AiInsightService {

    /**
     * @param generate true = 强制调用大模型重新生成；false = 回放最近一次结果（无结果时自动生成）
     * @return list / source(model|rule) / model / generatedAt / elapsedMs / cached /
     *         ageHours / expireLevel(fresh|aging|stale) / expireNote / fallbackReason / trendComment
     */
    Map<String, Object> getInsights(boolean generate);
}
