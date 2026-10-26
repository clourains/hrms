package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Overtime;
import org.apache.ibatis.annotations.Mapper;

/**
 * 加班申请 Mapper（P4-1）
 */
@Mapper
public interface OvertimeMapper extends BaseMapper<Overtime> {
}
