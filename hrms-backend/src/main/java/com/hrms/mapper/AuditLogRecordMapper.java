package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.AuditLogRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper（P1-1）
 */
@Mapper
public interface AuditLogRecordMapper extends BaseMapper<AuditLogRecord> {
}
