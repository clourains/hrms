package com.hrms.service;

import com.hrms.dto.BusinessInsightDTO.BusinessInsights;

/**
 * 业务侧智能分析
 *
 * 业务数据（销售预测 / 客户流失 / 库存补货 / 物流异常）始终由规则基于真实业务表计算；
 * generate=true 时额外调用大模型生成洞察文字与预测解读，失败自动回退规则结果。
 */
public interface BusinessInsightService {

    /**
     * 业务智能洞察：销售预测、客户流失预警、补货建议、物流异常与洞察列表。
     * 无数据时对应维度返回空数组/空对象，不抛异常。
     *
     * @param generate false：返回规则结果，若存在 24 小时内的 AI 结果则直接回放（source=model）；
     *                 true：强制调用模型重算，失败回退规则结果
     */
    BusinessInsights getBusinessInsights(boolean generate);
}
