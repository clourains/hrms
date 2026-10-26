package com.hrms.util;

/**
 * 登出 token 黑名单（P049）
 *
 * JWT 本身无状态，签发后无法主动作废；登出时把 token 登记到黑名单，
 * 过滤器校验时命中黑名单即返回 401，实现"退出登录后 token 立即失效"。
 *
 * M2-3 起提供两种实现，按配置 hrms.token-blacklist 装配（见 TokenBlacklistConfig）：
 * - RedisTokenBlacklist（默认）：集中式存储，重启/多实例均有效，TTL 自动清理；
 * - InMemoryTokenBlacklist：单实例内存兜底，供无 Redis 环境使用。
 */
public interface TokenBlacklist {

    /**
     * 登出时作废 token，保留至该 token 原本的过期时间
     */
    void revoke(String token);

    /**
     * token 是否已被登出作废
     */
    boolean isRevoked(String token);
}
