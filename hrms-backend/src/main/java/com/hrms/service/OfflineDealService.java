package com.hrms.service;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.OfflineDeal;

/**
 * 线下交易Service接口
 */
public interface OfflineDealService {

    PageResult<OfflineDeal> pageQuery(BizQueryDTO queryDTO);

    boolean save(OfflineDeal deal);

    boolean update(OfflineDeal deal);

    boolean updateStatus(Long id, Integer status);

    boolean deleteById(Long id);
}
