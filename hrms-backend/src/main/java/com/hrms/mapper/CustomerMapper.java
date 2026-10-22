package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 境外客户Mapper
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

}
