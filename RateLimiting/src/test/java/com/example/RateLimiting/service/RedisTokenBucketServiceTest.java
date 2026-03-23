package com.example.RateLimiting.service;

import com.example.RateLimiting.config.RateLimiterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Updated Unit tests for RedisTokenBucketService (Lua version).
 */
@ExtendWith(MockitoExtension.class)
class RedisTokenBucketServiceTest {

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    private RateLimiterProperties properties;
    private RedisTokenBucketService service;

    private static final String CLIENT_ID = "192.168.1.1";

    @BeforeEach
    void setUp() {
        properties = new RateLimiterProperties();
        properties.setCapacity(10);
        properties.setRefillRate(5);

        service = new RedisTokenBucketService(jedisPool, properties);
    }

    private void stubJedis() {
        lenient().when(jedisPool.getResource()).thenReturn(jedis);
    }

    @Test
    @DisplayName("isAllowed: returns true when Lua script returns remaining tokens (>= 0)")
    void isAllowed_allowed_returnsTrue() {
        stubJedis();
        // Mock Lua script returning 9 (remaining tokens after 10-1)
        when(jedis.eval(anyString(), anyList(), anyList())).thenReturn(9L);

        boolean allowed = service.isAllowed(CLIENT_ID);

        assertThat(allowed).isTrue();
        verify(jedis).eval(anyString(), anyList(), anyList());
    }

    @Test
    @DisplayName("isAllowed: returns false when Lua script returns -1 (blocked)")
    void isAllowed_blocked_returnsFalse() {
        stubJedis();
        // Mock Lua script returning -1
        when(jedis.eval(anyString(), anyList(), anyList())).thenReturn(-1L);

        boolean allowed = service.isAllowed(CLIENT_ID);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("getAvailableTokens: returns value from Lua script")
    void getAvailableTokens_returnsValue() {
        stubJedis();
        when(jedis.eval(anyString(), anyList(), anyList())).thenReturn(7L);

        long tokens = service.getAvailableTokens(CLIENT_ID);

        assertThat(tokens).isEqualTo(7L);
    }

    @Test
    @DisplayName("getCapacity returns configured value")
    void getCapacity_returnsConfiguredValue() {
        assertThat(service.getCapacity(CLIENT_ID)).isEqualTo(10L);
    }
}
