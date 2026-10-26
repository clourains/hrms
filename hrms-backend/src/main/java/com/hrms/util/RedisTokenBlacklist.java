package com.hrms.util;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Redis 版登出 token 黑名单（M2-3，默认实现）
 *
 * 以 hrms:token:blacklist:<token> 为键写入 Redis，TTL 即该 token 的剩余有效期，
 * 到期自动清理，无需懒清理逻辑；重启不丢失、支持多实例共享。
 */
public class RedisTokenBlacklist implements TokenBlacklist {

    private static final String KEY_PREFIX = "hrms:token:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public RedisTokenBlacklist(StringRedisTemplate redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void revoke(String token) {
        Date expiration = jwtUtil.getExpirationFromToken(token);
        if (expiration == null) {
            return;
        }
        long ttlSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        if (ttlSeconds <= 0) {
            // token 本身已过期，无需登记
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + token, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }
}
