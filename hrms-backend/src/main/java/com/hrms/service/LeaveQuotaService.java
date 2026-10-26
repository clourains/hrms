package com.hrms.service;

import com.hrms.entity.LeaveQuota;

import java.math.BigDecimal;

/**
 * 请假额度 Service（P4-2）
 */
public interface LeaveQuotaService {

    /** 年假类型标识 */
    String TYPE_ANNUAL = "年假";

    /** 默认年假天数（首次使用自动开池） */
    BigDecimal DEFAULT_ANNUAL_DAYS = new BigDecimal("10");

    /**
     * 取员工某年度某类型的额度，不存在则按默认值自动建池
     */
    LeaveQuota getOrCreate(Long empId, Integer year, String quotaType);

    /**
     * 审批通过后扣减额度（used_days 累加，允许超出 total 做提示用）
     */
    void deduct(Long empId, Integer year, String quotaType, BigDecimal days);

    /**
     * 年假剩余天数（不足时返回 0 起步的差值，可为负）
     */
    BigDecimal remainDays(Long empId, Integer year, String quotaType);

    /**
     * 当前年度
     */
    static int currentYear() {
        return java.time.LocalDate.now().getYear();
    }
}
