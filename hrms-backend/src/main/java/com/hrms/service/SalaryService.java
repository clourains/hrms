package com.hrms.service;

import com.hrms.entity.Salary;

import java.util.List;
import java.util.Map;

/**
 * 薪资Service接口
 */
public interface SalaryService {

    /**
     * 根据ID获取薪资记录
     *
     * @param id 薪资ID
     * @return 薪资记录
     */
    Salary getById(Long id);

    /**
     * 获取员工薪资列表
     *
     * @param empId 员工ID
     * @return 薪资列表
     */
    List<Salary> listByEmpId(Long empId);

    /**
     * 根据月份查询薪资
     *
     * @param salaryMonth 月份（格式：yyyy-MM）
     * @return 薪资列表
     */
    List<Salary> listByMonth(String salaryMonth);

    /**
     * 月度薪资统计
     *
     * @param salaryMonth 月份（格式：yyyy-MM）
     * @return 统计结果
     */
    Map<String, Object> monthlyStatistics(String salaryMonth);

    /**
     * 新增薪资记录
     *
     * @param salary 薪资记录
     * @return 是否成功
     */
    boolean save(Salary salary);

    /**
     * 更新薪资记录
     *
     * @param salary 薪资记录
     * @return 是否成功
     */
    boolean update(Salary salary);

    /**
     * 根据ID删除薪资记录
     *
     * @param id 薪资ID
     * @return 是否成功
     */
    boolean deleteById(Long id);
}
