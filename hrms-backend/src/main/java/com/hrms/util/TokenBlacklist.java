package com.hrms.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登出 token 黑名单（P049）
 *
 * JWT 本身无状态，签发后无法主动作废；登出时把 token 登记到黑名单，
 * 拦截器校验时命中黑名单即返回 401，实现"退出登录后 token 立即失效"。
 *
 * 条目只需保留到该 token 原本的过期时间（之后 JWT 自然失效），登记时顺带懒清理过期条目。
 * 当前为单实例内存实现，M2 引入 Redis 后可平滑替换为集中式存储。
 */
@Component
public class TokenBlacklist {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * token -> 该 token 的过期时间戳（毫秒）
     */
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    /**
     * 登出时作废 token，保留至该 token 原本的过期时间
     */
    public void revoke(String token) {
        Date expiration = jwtUtil.getExpirationFromToken(token);
        if (expiration == null) {
            return;
        }
        evictExpired();
        revokedTokens.put(token, expiration.getTime());
    }

    /**
     * token 是否已被登出作废
     */
    public boolean isRevoked(String token) {
        Long expiresAt = revokedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            // 已超过 token 原过期时间，JWT 本身也失效了，条目可删
            revokedTokens.remove(token);
            return false;
        }
        return true;
    }

    /**
     * 懒清理：已过期条目对应的 token 本来就无法通过校验，直接移除
     */
    private void evictExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = revokedTokens.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() < now) {
                iterator.remove();
            }
        }
    }
}
