package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Supplier;

import java.util.List;

/**
 * 供应商Service接口
 */
public interface SupplierService {

    PageResult<Supplier> pageQuery(BizQueryDTO queryDTO);

    Supplier getById(Long id);

    boolean save(Supplier supplier);

    boolean update(Supplier supplier);

    boolean deleteById(Long id);

    List<Supplier> listAll();
}
