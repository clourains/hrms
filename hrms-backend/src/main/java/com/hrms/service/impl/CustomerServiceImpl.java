package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Customer;
import com.hrms.mapper.CustomerMapper;
import com.hrms.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 境外客户Service实现类
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public PageResult<Customer> pageQuery(BizQueryDTO queryDTO) {
        Page<Customer> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Customer::getCompanyName, queryDTO.getKeyword())
                    .or().like(Customer::getCustomerCode, queryDTO.getKeyword())
                    .or().like(Customer::getContactName, queryDTO.getKeyword()));
        }
        if (StringUtils.hasText(queryDTO.getCountry())) {
            wrapper.eq(Customer::getCountry, queryDTO.getCountry());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Customer::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByDesc(Customer::getCreateTime);
        Page<Customer> result = customerMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Customer getById(Long id) {
        return customerMapper.selectById(id);
    }

    @Override
    public boolean save(Customer customer) {
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        return customerMapper.insert(customer) > 0;
    }

    @Override
    public boolean update(Customer customer) {
        customer.setUpdateTime(LocalDateTime.now());
        return customerMapper.updateById(customer) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return customerMapper.deleteById(id) > 0;
    }

    @Override
    public List<Customer> listAll() {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getStatus, 1).orderByAsc(Customer::getCustomerCode);
        return customerMapper.selectList(wrapper);
    }
}
