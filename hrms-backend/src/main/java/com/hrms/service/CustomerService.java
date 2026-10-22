package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Customer;

import java.util.List;

/**
 * 境外客户Service接口
 */
public interface CustomerService {

    PageResult<Customer> pageQuery(BizQueryDTO queryDTO);

    Customer getById(Long id);

    boolean save(Customer customer);

    boolean update(Customer customer);

    boolean deleteById(Long id);

    List<Customer> listAll();
}
