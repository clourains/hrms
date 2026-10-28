package com.hrms.service;

import com.hrms.dto.EmployeeExitQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.EmployeeExit;

import java.util.Map;

/**
 * 入离管理（离职 / 辞退）Service
 */
public interface EmployeeExitService {

    PageResult<EmployeeExit> page(EmployeeExitQueryDTO query);

    /** kind 为 null 时统计全部，1 只看离职，2 只看辞退 */
    Map<String, Object> statistics(Integer kind);

    EmployeeExit create(EmployeeExit exit);

    void update(EmployeeExit exit);

    void approve(Long id, String remark);

    void reject(Long id, String remark);

    /** 生效：员工置离职、写离职日期、停用登录账号 */
    void effect(Long id);

    void cancel(Long id);

    void delete(Long id);
}
