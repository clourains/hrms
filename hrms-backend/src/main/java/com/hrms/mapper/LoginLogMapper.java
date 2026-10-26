package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志Mapper（P5-2）
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
