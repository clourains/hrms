package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Invoice;

/**
 * 发票Service接口
 */
public interface InvoiceService {

    PageResult<Invoice> pageQuery(BizQueryDTO queryDTO);

    /**
     * 从销售订单开票：订单号、客户、金额默认取订单数据
     */
    Invoice createFromOrder(Invoice invoice);

    boolean update(Invoice invoice);

    boolean updateStatus(Long id, Integer status);

    boolean deleteById(Long id);
}
