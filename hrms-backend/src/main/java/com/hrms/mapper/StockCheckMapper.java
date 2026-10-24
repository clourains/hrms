package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.StockCheck;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存盘点单Mapper（P019）
 */
@Mapper
public interface StockCheckMapper extends BaseMapper<StockCheck> {
}
