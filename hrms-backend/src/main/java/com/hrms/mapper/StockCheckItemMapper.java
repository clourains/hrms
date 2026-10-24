package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.StockCheckItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存盘点单明细Mapper（P019）
 */
@Mapper
public interface StockCheckItemMapper extends BaseMapper<StockCheckItem> {
}
