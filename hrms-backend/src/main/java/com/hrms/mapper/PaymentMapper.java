package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收付款Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

}
