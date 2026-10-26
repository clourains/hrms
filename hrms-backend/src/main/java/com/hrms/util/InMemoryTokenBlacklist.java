package com.hrms.util;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版登出 token 黑名单（P049 兜底实现）
 *
 * 条目只需保留到该 token 原本的过期时间（之后 JWT 自然失效），登记时顺带懒清理过期条目。
 * 仅适用于单实例开发环境，生产/多实例请用 RedisTokenBlacklist。
 */
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final JwtUtil jwtUtil;

    public InMemoryTokenBlacklist(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * token -> 该 token 的过期时间戳（毫秒）
     */
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    @Override
    public void revoke(String token) {
        Date expiration = jwtUtil.getExpirationFromToken(token);
        if (expiration == null) {
            return;
        }
        evictExpired();
        revokedTokens.put(token, expiration.getTime());
    }

    @Override
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
