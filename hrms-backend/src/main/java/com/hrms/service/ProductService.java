package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Product;

import java.util.List;

/**
 * 产品Service接口
 */
public interface ProductService {

    PageResult<Product> pageQuery(BizQueryDTO queryDTO);

    Product getById(Long id);

    boolean save(Product product);

    boolean update(Product product);

    boolean deleteById(Long id);

    List<Product> listAll();

    /**
     * 产品下拉列表
     *
     * @param includeDisabled true 时连下架产品一起返回，出入库/订单下拉用（P021）
     */
    List<Product> listAll(boolean includeDisabled);
}
