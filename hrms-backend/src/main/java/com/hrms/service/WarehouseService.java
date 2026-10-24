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

    /**
     * 仓库下拉列表（只返回启用的仓库，供业务下拉框使用）
     */
    List<Warehouse> listAll();

    /**
     * 仓库列表
     *
     * @param onlyEnabled true 只返回启用的（下拉框用）；false 返回全部（仓库管理页用）
     */
    List<Warehouse> listAll(boolean onlyEnabled);
}
