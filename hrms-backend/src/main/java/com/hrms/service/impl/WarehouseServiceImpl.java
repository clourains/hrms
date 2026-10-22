package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Warehouse;
import com.hrms.mapper.WarehouseMapper;
import com.hrms.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仓库Service实现类
 */
@Service
public class WarehouseServiceImpl implements WarehouseService {

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public PageResult<Warehouse> pageQuery(BizQueryDTO queryDTO) {
        Page<Warehouse> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Warehouse::getWarehouseName, queryDTO.getKeyword())
                    .or().like(Warehouse::getWarehouseCode, queryDTO.getKeyword())
                    .or().like(Warehouse::getCity, queryDTO.getKeyword()));
        }
        if (queryDTO.getType() != null) {
            wrapper.eq(Warehouse::getType, queryDTO.getType());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Warehouse::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByAsc(Warehouse::getWarehouseCode);
        Page<Warehouse> result = warehouseMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Warehouse getById(Long id) {
        return warehouseMapper.selectById(id);
    }

    @Override
    public boolean save(Warehouse warehouse) {
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseMapper.insert(warehouse) > 0;
    }

    @Override
    public boolean update(Warehouse warehouse) {
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseMapper.updateById(warehouse) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return warehouseMapper.deleteById(id) > 0;
    }

    @Override
    public List<Warehouse> listAll() {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Warehouse::getStatus, 1).orderByAsc(Warehouse::getWarehouseCode);
        return warehouseMapper.selectList(wrapper);
    }
}
