package com.example.RateLimiting.service;

import org.springframework.stereotype.Service;
import com.example.RateLimiting.config.RateLimiterProperties;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisTokenBucketService {

    private final JedisPool jedisPool;
    private final RateLimiterProperties properties;

    private static final String TOKEN_KEY_PREFIX = "rate_limiter:tokens:";
    private static final String LAST_REFILL_KEY_PREFIX = "rate_limiter:last_refill:";

    /**
     * LUA script to perform atomic refill and decrement.
     * Logic:
     * 1. Check last refill time.
     * 2. Calculate tokens to add based on elapsed time.
     * 3. Update token count (capped at capacity).
     * 4. If tokens >= requested, decrement and return remaining.
     * 5. Otherwise return -1.
     */
    private static final String LUA_RATE_LIMITER = "local tokens_key = KEYS[1] " +
            "local timestamp_key = KEYS[2] " +
            "local capacity = tonumber(ARGV[1]) " +
            "local refill_rate = tonumber(ARGV[2]) " +
            "local now = tonumber(ARGV[3]) " +
            "local requested = tonumber(ARGV[4]) " +

            "local last_refill = tonumber(redis.call('get', timestamp_key) or 0) " +
            "local current_tokens = tonumber(redis.call('get', tokens_key) or capacity) " +

            "if last_refill == 0 then " +
            "  redis.call('set', tokens_key, capacity - requested) " +
            "  redis.call('set', timestamp_key, now) " +
            "  return capacity - requested " +
            "end " +

            "local elapsed = math.max(0, now - last_refill) " +
            "local refill = math.floor(elapsed * refill_rate / 1000) " +
            "local new_tokens = math.min(capacity, current_tokens + refill) " +

            "if new_tokens >= requested then " +
            "  local remaining = new_tokens - requested " +
            "  redis.call('set', tokens_key, remaining) " +
            "  redis.call('set', timestamp_key, now) " +
            "  return remaining " +
            "else " +
            "  return -1 " +
            "end";

    public boolean isAllowed(String clientId) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        String refillKey = LAST_REFILL_KEY_PREFIX + clientId;

        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.eval(LUA_RATE_LIMITER,
                    List.of(tokenKey, refillKey),
                    List.of(
                            String.valueOf(properties.getCapacity()),
                            String.valueOf(properties.getRefillRate()),
                            String.valueOf(System.currentTimeMillis()),
                            "1" // requested tokens
                    ));

            long remaining = (Long) result;
            return remaining >= 0;
        }
    }

    public long getCapacity(String clientId) {
        return properties.getCapacity();
    }

    public long getAvailableTokens(String clientId) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        String refillKey = LAST_REFILL_KEY_PREFIX + clientId;

        // We can use a similar Lua script to just calculate available tokens without
        // consuming
        String luaGetAvailable = "local tokens_key = KEYS[1] " +
                "local timestamp_key = KEYS[2] " +
                "local capacity = tonumber(ARGV[1]) " +
                "local refill_rate = tonumber(ARGV[2]) " +
                "local now = tonumber(ARGV[3]) " +
                "local last_refill = tonumber(redis.call('get', timestamp_key) or 0) " +
                "if last_refill == 0 then return capacity end " +
                "local current_tokens = tonumber(redis.call('get', tokens_key) or capacity) " +
                "local elapsed = math.max(0, now - last_refill) " +
                "local refill = math.floor(elapsed * refill_rate / 1000) " +
                "return math.min(capacity, current_tokens + refill)";

        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.eval(luaGetAvailable,
                    List.of(tokenKey, refillKey),
                    List.of(
                            String.valueOf(properties.getCapacity()),
                            String.valueOf(properties.getRefillRate()),
                            String.valueOf(System.currentTimeMillis())));
            return (Long) result;
        }
    }
}
