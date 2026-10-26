package com.hrms.config;

import com.hrms.util.InMemoryTokenBlacklist;
import com.hrms.util.JwtUtil;
import com.hrms.util.RedisTokenBlacklist;
import com.hrms.util.TokenBlacklist;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * token 黑名单装配（M2-3）
 *
 * hrms.token-blacklist=redis（默认）：Redis 集中式黑名单；
 * hrms.token-blacklist=memory：无 Redis 环境切换为内存黑名单。
 */
@Configuration
public class TokenBlacklistConfig {

    @Bean
    @ConditionalOnProperty(name = "hrms.token-blacklist", havingValue = "redis", matchIfMissing = true)
    public TokenBlacklist redisTokenBlacklist(StringRedisTemplate redisTemplate, JwtUtil jwtUtil) {
        return new RedisTokenBlacklist(redisTemplate, jwtUtil);
    }

    @Bean
    @ConditionalOnProperty(name = "hrms.token-blacklist", havingValue = "memory")
    public TokenBlacklist inMemoryTokenBlacklist(JwtUtil jwtUtil) {
        return new InMemoryTokenBlacklist(jwtUtil);
    }
}
