package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.InventoryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存变动记录Mapper
 */
@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {

}
