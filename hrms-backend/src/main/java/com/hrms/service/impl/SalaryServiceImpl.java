package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Salary;
import com.hrms.mapper.SalaryMapper;
import com.hrms.service.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 薪资Service实现类
 */
@Service
public class SalaryServiceImpl implements SalaryService {

    @Autowired
    private SalaryMapper salaryMapper;

    @Override
    public Salary getById(Long id) {
        return salaryMapper.selectById(id);
    }

    @Override
    public List<Salary> listByEmpId(Long empId) {
        LambdaQueryWrapper<Salary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Salary::getEmpId, empId)
                .orderByDesc(Salary::getSalaryMonth);
        return salaryMapper.selectList(queryWrapper);
    }

    @Override
    public List<Salary> listByMonth(String salaryMonth) {
        LambdaQueryWrapper<Salary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Salary::getSalaryMonth, salaryMonth)
                .orderByAsc(Salary::getEmpId);
        return salaryMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, Object> monthlyStatistics(String salaryMonth) {
        List<Salary> list = listByMonth(salaryMonth);

        Map<String, Object> result = new HashMap<>();
        BigDecimal totalBaseSalary = BigDecimal.ZERO;
        BigDecimal totalPerformanceBonus = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalAllowance = BigDecimal.ZERO;
        BigDecimal totalSocialSecurity = BigDecimal.ZERO;
        BigDecimal totalHousingFund = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalOtherDeduction = BigDecimal.ZERO;
        BigDecimal totalNetSalary = BigDecimal.ZERO;

        for (Salary salary : list) {
            if (salary.getBaseSalary() != null) {
                totalBaseSalary = totalBaseSalary.add(salary.getBaseSalary());
            }
            if (salary.getPerformanceBonus() != null) {
                totalPerformanceBonus = totalPerformanceBonus.add(salary.getPerformanceBonus());
            }
            if (salary.getOvertimePay() != null) {
                totalOvertimePay = totalOvertimePay.add(salary.getOvertimePay());
            }
            if (salary.getAllowance() != null) {
                totalAllowance = totalAllowance.add(salary.getAllowance());
            }
            if (salary.getSocialSecurity() != null) {
                totalSocialSecurity = totalSocialSecurity.add(salary.getSocialSecurity());
            }
            if (salary.getHousingFund() != null) {
                totalHousingFund = totalHousingFund.add(salary.getHousingFund());
            }
            if (salary.getTax() != null) {
                totalTax = totalTax.add(salary.getTax());
            }
            if (salary.getOtherDeduction() != null) {
                totalOtherDeduction = totalOtherDeduction.add(salary.getOtherDeduction());
            }
            if (salary.getNetSalary() != null) {
                totalNetSalary = totalNetSalary.add(salary.getNetSalary());
            }
        }

        result.put("employeeCount", list.size());
        result.put("totalBaseSalary", totalBaseSalary.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalPerformanceBonus", totalPerformanceBonus.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalOvertimePay", totalOvertimePay.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalAllowance", totalAllowance.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalSocialSecurity", totalSocialSecurity.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalHousingFund", totalHousingFund.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalTax", totalTax.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalOtherDeduction", totalOtherDeduction.setScale(2, BigDecimal.ROUND_HALF_UP));
        result.put("totalNetSalary", totalNetSalary.setScale(2, BigDecimal.ROUND_HALF_UP));

        return result;
    }

    @Override
    public boolean save(Salary salary) {
        // 自动计算实发工资
        calculateNetSalary(salary);
        salary.setCreateTime(LocalDateTime.now());
        salary.setUpdateTime(LocalDateTime.now());
        return salaryMapper.insert(salary) > 0;
    }

    @Override
    public boolean update(Salary salary) {
        calculateNetSalary(salary);
        salary.setUpdateTime(LocalDateTime.now());
        return salaryMapper.updateById(salary) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return salaryMapper.deleteById(id) > 0;
    }

    /**
     * 计算实发工资
     */
    private void calculateNetSalary(Salary salary) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalDeduction = BigDecimal.ZERO;

        if (salary.getBaseSalary() != null) {
            totalIncome = totalIncome.add(salary.getBaseSalary());
        }
        if (salary.getPerformanceBonus() != null) {
            totalIncome = totalIncome.add(salary.getPerformanceBonus());
        }
        if (salary.getOvertimePay() != null) {
            totalIncome = totalIncome.add(salary.getOvertimePay());
        }
        if (salary.getAllowance() != null) {
            totalIncome = totalIncome.add(salary.getAllowance());
        }
        if (salary.getSocialSecurity() != null) {
            totalDeduction = totalDeduction.add(salary.getSocialSecurity());
        }
        if (salary.getHousingFund() != null) {
            totalDeduction = totalDeduction.add(salary.getHousingFund());
        }
        if (salary.getTax() != null) {
            totalDeduction = totalDeduction.add(salary.getTax());
        }
        if (salary.getOtherDeduction() != null) {
            totalDeduction = totalDeduction.add(salary.getOtherDeduction());
        }

        salary.setNetSalary(totalIncome.subtract(totalDeduction).setScale(2, BigDecimal.ROUND_HALF_UP));
    }
}
