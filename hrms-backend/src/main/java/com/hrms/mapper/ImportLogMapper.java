package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.ImportLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 批量导入审计日志Mapper
 */
@Mapper
public interface ImportLogMapper extends BaseMapper<ImportLog> {
}
