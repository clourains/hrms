package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.LeaveQuota;
import com.hrms.mapper.LeaveQuotaMapper;
import com.hrms.service.LeaveQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假额度 Service 实现（P4-2）
 */
@Service
public class LeaveQuotaServiceImpl implements LeaveQuotaService {

    @Autowired
    private LeaveQuotaMapper leaveQuotaMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveQuota getOrCreate(Long empId, Integer year, String quotaType) {
        LeaveQuota quota = leaveQuotaMapper.selectOne(new LambdaQueryWrapper<LeaveQuota>()
                .eq(LeaveQuota::getEmpId, empId)
                .eq(LeaveQuota::getYear, year)
                .eq(LeaveQuota::getQuotaType, quotaType));
        if (quota != null) {
            return quota;
        }
        LeaveQuota created = new LeaveQuota();
        created.setEmpId(empId);
        created.setYear(year);
        created.setQuotaType(quotaType);
        BigDecimal total = TYPE_ANNUAL.equals(quotaType) ? DEFAULT_ANNUAL_DAYS : BigDecimal.ZERO;
        created.setTotalDays(total);
        created.setUsedDays(BigDecimal.ZERO);
        LocalDateTime now = LocalDateTime.now();
        created.setCreateTime(now);
        created.setUpdateTime(now);
        leaveQuotaMapper.insert(created);
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long empId, Integer year, String quotaType, BigDecimal days) {
        if (empId == null || days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        LeaveQuota quota = getOrCreate(empId, year, quotaType);
        quota.setUsedDays(quota.getUsedDays().add(days));
        quota.setUpdateTime(LocalDateTime.now());
        leaveQuotaMapper.updateById(quota);
    }

    @Override
    public BigDecimal remainDays(Long empId, Integer year, String quotaType) {
        LeaveQuota quota = getOrCreate(empId, year, quotaType);
        return quota.getTotalDays().subtract(quota.getUsedDays());
    }
}
