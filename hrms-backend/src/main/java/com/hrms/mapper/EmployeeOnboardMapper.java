package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.EmployeeOnboard;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工入职登记 Mapper
 */
@Mapper
public interface EmployeeOnboardMapper extends BaseMapper<EmployeeOnboard> {
}
