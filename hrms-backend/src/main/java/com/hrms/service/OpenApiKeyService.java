package com.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.SysConfig;
import com.hrms.mapper.SysConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * 开放接口访问密钥（存放在 sys_config，只有管理员能在参数配置里看到）
 */
@Service
public class OpenApiKeyService {

    /** sys_config 里的参数键 */
    private static final String CONFIG_KEY = "openapi.accessKey";

    @Autowired
    private SysConfigMapper sysConfigMapper;

    /**
     * 取当前密钥，没有则自动生成一个
     */
    public String getOrCreate() {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, CONFIG_KEY));
        if (config != null && StringUtils.hasText(config.getConfigValue())) {
            return config.getConfigValue();
        }

        String key = generateKey();
        if (config == null) {
            SysConfig created = new SysConfig();
            created.setConfigKey(CONFIG_KEY);
            created.setConfigValue(key);
            created.setRemark("开放接口访问密钥（请求头 X-API-Key）");
            sysConfigMapper.insert(created);
        } else {
            config.setConfigValue(key);
            sysConfigMapper.updateById(config);
        }
        return key;
    }

    /**
     * 重置密钥：旧 Key 立即失效
     */
    public String rotate() {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, CONFIG_KEY));
        String key = generateKey();
        if (config == null) {
            SysConfig created = new SysConfig();
            created.setConfigKey(CONFIG_KEY);
            created.setConfigValue(key);
            created.setRemark("开放接口访问密钥（请求头 X-API-Key）");
            sysConfigMapper.insert(created);
        } else {
            config.setConfigValue(key);
            sysConfigMapper.updateById(config);
        }
        return key;
    }

    /**
     * 校验请求头里的 Key 是否有效
     *
     * 用常量时间比较，避免通过响应耗时逐位猜密钥
     */
    public boolean isValid(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String current = getOrCreate();
        return MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8),
                current.getBytes(StandardCharsets.UTF_8));
    }

    private String generateKey() {
        return "hrms_" + UUID.randomUUID().toString().replace("-", "");
    }
}
