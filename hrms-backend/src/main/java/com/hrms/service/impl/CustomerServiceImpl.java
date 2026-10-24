package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Customer;
import com.hrms.entity.TradeOrder;
import com.hrms.mapper.CustomerMapper;
import com.hrms.mapper.TradeOrderMapper;
import com.hrms.service.CustomerService;
import com.hrms.util.CodeReleaser;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 境外客户Service实现类
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    /** 客户列表可排序字段白名单（P004） */
    private static final Map<String, SortUtil.OrderCol> CUSTOMER_SORT = new HashMap<>();

    static {
        CUSTOMER_SORT.put("customerCode", SortUtil.OrderCol.natural("customer_code"));
        CUSTOMER_SORT.put("companyName", SortUtil.OrderCol.natural("company_name"));
        CUSTOMER_SORT.put("country", SortUtil.OrderCol.natural("country"));
        CUSTOMER_SORT.put("city", SortUtil.OrderCol.natural("city"));
        CUSTOMER_SORT.put("status", SortUtil.OrderCol.plain("status"));
        CUSTOMER_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

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

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), CUSTOMER_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(Customer::getCreateTime);
        }
        Page<Customer> result = customerMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Customer getById(Long id) {
        return customerMapper.selectById(id);
    }

    @Override
    public boolean save(Customer customer) {
        Validators.text(customer.getCustomerCode(), "客户编码");
        Validators.text(customer.getCompanyName(), "公司名称");
        Validators.text(customer.getContactName(), "联系人");
        Validators.text(customer.getPhone(), "电话");
        Validators.text(customer.getCountry(), "国家");
        Validators.text(customer.getCity(), "城市");
        Validators.text(customer.getAddress(), "地址");
        Validators.nonNegative(customer.getCreditLimitUsd(), "信用额度");
        if (customer.getStatus() == null) {
            customer.setStatus(1);
        }
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        return customerMapper.insert(customer) > 0;
    }

    @Override
    public boolean update(Customer customer) {
        if (customer.getId() == null) {
            throw new RuntimeException("客户不存在");
        }
        Customer origin = customerMapper.selectById(customer.getId());
        if (origin == null) {
            throw new RuntimeException("客户不存在或已删除");
        }
        Validators.notCleared(origin.getCustomerCode(), customer.getCustomerCode(), "客户编码");
        Validators.notCleared(origin.getCompanyName(), customer.getCompanyName(), "公司名称");
        Validators.notCleared(origin.getContactName(), customer.getContactName(), "联系人");
        Validators.notCleared(origin.getPhone(), customer.getPhone(), "电话");
        Validators.notCleared(origin.getCountry(), customer.getCountry(), "国家");
        Validators.notCleared(origin.getCity(), customer.getCity(), "城市");
        Validators.notCleared(origin.getAddress(), customer.getAddress(), "地址");
        Validators.nonNegative(customer.getCreditLimitUsd(), "信用额度");
        customer.setUpdateTime(LocalDateTime.now());
        return customerMapper.updateById(customer) > 0;
    }

    /**
     * 删除客户（P043、P048）
     *
     * P048：客户被订单引用时不允许删除。订单列表的客户名是靠 customer_id 现查的，
     * 而 @TableLogic 会把已删客户过滤掉，删完历史订单的客户名就变成空白，订单归属再也查不回来。
     * 不再合作应该改状态为「停用」，而不是把主数据删掉。
     *
     * P043：逻辑删除只是把 deleted 置 1，行还在、编码还被唯一索引占着，
     * 同一编码再也建不出来。所以删之前先把编码让出来（见 CodeReleaser）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new RuntimeException("客户不存在或已删除");
        }

        LambdaQueryWrapper<TradeOrder> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(TradeOrder::getCustomerId, id);
        Long refCount = tradeOrderMapper.selectCount(refWrapper);
        if (refCount != null && refCount > 0) {
            throw new RuntimeException("客户「" + customer.getCompanyName() + "」下还有 " + refCount
                    + " 张订单，删除后这些订单的客户信息会丢失。如果只是不再合作，"
                    + "请把客户状态改为「停用」");
        }

        Customer release = new Customer();
        release.setId(id);
        release.setCustomerCode(CodeReleaser.release(customer.getCustomerCode(), id, 50));
        release.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(release);

        return customerMapper.deleteById(id) > 0;
    }

    @Override
    public List<Customer> listAll() {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getStatus, 1).orderByAsc(Customer::getCustomerCode);
        return customerMapper.selectList(wrapper);
    }
}
