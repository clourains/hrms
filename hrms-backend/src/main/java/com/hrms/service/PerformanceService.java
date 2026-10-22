package com.hrms.service;

import com.hrms.entity.Performance;

import java.util.List;
import java.util.Map;

/**
 * 绩效Service接口
 */
public interface PerformanceService {

    /**
     * 根据ID获取绩效记录
     *
     * @param id 绩效ID
     * @return 绩效记录
     */
    Performance getById(Long id);

    /**
     * 获取员工绩效列表
     *
     * @param empId 员工ID
     * @return 绩效列表
     */
    List<Performance> listByEmpId(Long empId);

    /**
     * 根据周期查询绩效
     *
     * @param period 周期
     * @return 绩效列表
     */
    List<Performance> listByPeriod(String period);

    /**
     * 绩效评估
     *
     * @param performance 绩效记录
     * @return 是否成功
     */
    boolean evaluate(Performance performance);

    /**
     * 新增绩效记录
     *
     * @param performance 绩效记录
     * @return 是否成功
     */
    boolean save(Performance performance);

    /**
     * 更新绩效记录
     *
     * @param performance 绩效记录
     * @return 是否成功
     */
    boolean update(Performance performance);

    /**
     * 根据ID删除绩效记录
     *
     * @param id 绩效ID
     * @return 是否成功
     */
    boolean deleteById(Long id);

    /**
     * 绩效统计
     *
     * @param period 周期
     * @return 统计结果
     */
    Map<String, Object> statistics(String period);
}
