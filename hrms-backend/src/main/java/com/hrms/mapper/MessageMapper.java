package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内信 Mapper（P2-1）
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
