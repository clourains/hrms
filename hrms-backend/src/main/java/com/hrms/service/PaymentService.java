package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Payment;

import java.util.Map;

/**
 * 收付款Service接口
 */
public interface PaymentService {

    PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO);

    Payment getById(Long id);

    /**
     * 新增收付款，并联动更新订单付款状态
     */
    boolean save(Payment payment);

    boolean deleteById(Long id);
}
