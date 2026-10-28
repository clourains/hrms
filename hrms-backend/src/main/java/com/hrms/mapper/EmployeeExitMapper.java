package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.EmployeeExit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工离职/辞退单据 Mapper
 */
@Mapper
public interface EmployeeExitMapper extends BaseMapper<EmployeeExit> {
}
