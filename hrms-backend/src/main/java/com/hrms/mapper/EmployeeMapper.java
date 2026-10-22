package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工Mapper
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

}
