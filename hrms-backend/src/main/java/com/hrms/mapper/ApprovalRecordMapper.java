package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批节点记录 Mapper（P3-1）
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {
}
