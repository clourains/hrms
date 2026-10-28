package com.hrms.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 开放接口限流：按来源 IP 每分钟最多 60 次写入请求
 *
 * 单实例进程内计数即可满足本系统规模；多实例部署时应换成 Redis 计数。
 */
@Service
public class OpenApiRateLimiter {

    private static final int LIMIT_PER_WINDOW = 60;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_TRACKED_IPS = 20_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @return true 放行，false 触发限流
     */
    public boolean allow(String identity) {
        if (windows.size() > MAX_TRACKED_IPS) {
            windows.clear();
        }
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(identity, k -> new Window(now));
        synchronized (window) {
            if (now - window.start >= WINDOW_MILLIS) {
                window.start = now;
                window.count.set(0);
            }
            return window.count.incrementAndGet() <= LIMIT_PER_WINDOW;
        }
    }

    private static final class Window {
        private volatile long start;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(long start) {
            this.start = start;
        }
    }
}
