package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Warehouse;

import java.util.List;

/**
 * 仓库Service接口
 */
public interface WarehouseService {

    PageResult<Warehouse> pageQuery(BizQueryDTO queryDTO);

    Warehouse getById(Long id);

    boolean save(Warehouse warehouse);

    boolean update(Warehouse warehouse);

    boolean deleteById(Long id);

    List<Warehouse> listAll();
}
