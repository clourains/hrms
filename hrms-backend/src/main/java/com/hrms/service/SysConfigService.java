package com.hrms.service;

import com.hrms.entity.SysConfig;

import java.math.BigDecimal;
import java.util.List;

/**
 * 系统参数配置Service接口（P5-3）
 */
public interface SysConfigService {

    /**
     * 查询全部参数
     */
    List<SysConfig> listAll();

    /**
     * 按 key 取参数值，取不到时返回默认值
     */
    String getValue(String key, String defaultValue);

    /**
     * 按 key 取数值参数，取不到或格式错误时返回默认值
     */
    BigDecimal getDecimal(String key, BigDecimal defaultValue);

    /**
     * 更新参数值（仅允许更新已存在的 key，防止调用方拼错静默新增）
     *
     * @param operator 操作人
     */
    void updateValue(String key, String value, String operator);
}
