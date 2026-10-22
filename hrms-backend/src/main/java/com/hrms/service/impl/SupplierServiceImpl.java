package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Supplier;
import com.hrms.mapper.SupplierMapper;
import com.hrms.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商Service实现类
 */
@Service
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierMapper supplierMapper;

    @Override
    public PageResult<Supplier> pageQuery(BizQueryDTO queryDTO) {
        Page<Supplier> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Supplier::getCompanyName, queryDTO.getKeyword())
                    .or().like(Supplier::getSupplierCode, queryDTO.getKeyword())
                    .or().like(Supplier::getCategory, queryDTO.getKeyword()));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Supplier::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByDesc(Supplier::getCreateTime);
        Page<Supplier> result = supplierMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Supplier getById(Long id) {
        return supplierMapper.selectById(id);
    }

    @Override
    public boolean save(Supplier supplier) {
        supplier.setCreateTime(LocalDateTime.now());
        supplier.setUpdateTime(LocalDateTime.now());
        return supplierMapper.insert(supplier) > 0;
    }

    @Override
    public boolean update(Supplier supplier) {
        supplier.setUpdateTime(LocalDateTime.now());
        return supplierMapper.updateById(supplier) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return supplierMapper.deleteById(id) > 0;
    }

    @Override
    public List<Supplier> listAll() {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getStatus, 1).orderByAsc(Supplier::getSupplierCode);
        return supplierMapper.selectList(wrapper);
    }
}
