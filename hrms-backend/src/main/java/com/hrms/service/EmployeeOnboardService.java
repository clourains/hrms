package com.hrms.service;

import com.hrms.dto.EmployeeOnboardQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.EmployeeOnboard;

import java.util.Map;

/**
 * 入职登记 Service
 */
public interface EmployeeOnboardService {

    PageResult<EmployeeOnboard> page(EmployeeOnboardQueryDTO query);

    Map<String, Object> statistics();

    EmployeeOnboard create(EmployeeOnboard onboard);

    void update(EmployeeOnboard onboard);

    /** 关联已有员工档案 */
    void link(Long id, Long employeeId);

    /** 一键新建员工档案并自动生成试用期记录，返回新档案ID */
    Long createProfile(Long id);

    void delete(Long id);
}
