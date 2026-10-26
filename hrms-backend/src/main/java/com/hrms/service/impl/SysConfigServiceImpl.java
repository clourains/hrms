package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.SysConfig;
import com.hrms.mapper.SysConfigMapper;
import com.hrms.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统参数配置Service实现类（P5-3）
 *
 * 带进程内缓存：参数读取远多于修改，缓存避免每次算薪/导出都查库；
 * 更新时同步失效缓存，单实例部署下足够可靠。
 */
@Service
public class SysConfigServiceImpl implements SysConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public List<SysConfig> listAll() {
        return sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getConfigKey));
    }

    @Override
    public String getValue(String key, String defaultValue) {
        String value = cache.computeIfAbsent(key, k -> {
            SysConfig config = sysConfigMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, k));
            return config != null ? config.getConfigValue() : "";
        });
        return value.isEmpty() ? defaultValue : value;
    }

    @Override
    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String value = getValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void updateValue(String key, String value, String operator) {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config == null) {
            throw new RuntimeException("参数不存在：" + key);
        }
        config.setConfigValue(value);
        config.setUpdateBy(operator);
        config.setUpdateTime(LocalDateTime.now());
        sysConfigMapper.updateById(config);
        cache.remove(key);
    }
}
