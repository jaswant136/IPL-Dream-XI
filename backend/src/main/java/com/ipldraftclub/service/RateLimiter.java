package com.ipldraftclub.service;

import com.ipldraftclub.exception.ApiException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory sliding-window limiter for expensive operations (starting drafts).
 * Per-instance only: behind several instances use a shared limiter (API gateway / Redis) instead.
 */
@Component
public class RateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(String key, int maxPerMinute) {
        if (maxPerMinute <= 0) {
            return;
        }
        long now = clock.millis();
        Deque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MILLIS) {
                window.pollFirst();
            }
            if (window.size() >= maxPerMinute) {
                throw ApiException.tooManyRequests("You are doing that too quickly. Please wait a moment and try again.");
            }
            window.addLast(now);
        }
    }
}
