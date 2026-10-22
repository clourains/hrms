package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Product;
import com.hrms.mapper.ProductMapper;
import com.hrms.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 产品Service实现类
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public PageResult<Product> pageQuery(BizQueryDTO queryDTO) {
        Page<Product> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Product::getName, queryDTO.getKeyword())
                    .or().like(Product::getNameEn, queryDTO.getKeyword())
                    .or().like(Product::getSku, queryDTO.getKeyword())
                    .or().like(Product::getCategory, queryDTO.getKeyword()));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Product::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = productMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public boolean save(Product product) {
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        return productMapper.insert(product) > 0;
    }

    @Override
    public boolean update(Product product) {
        product.setUpdateTime(LocalDateTime.now());
        return productMapper.updateById(product) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return productMapper.deleteById(id) > 0;
    }

    @Override
    public List<Product> listAll() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1).orderByAsc(Product::getSku);
        return productMapper.selectList(wrapper);
    }
}
