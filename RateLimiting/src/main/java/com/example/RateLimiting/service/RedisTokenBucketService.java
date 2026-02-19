package com.example.RateLimiting.service;

import org.springframework.stereotype.Service;

import com.example.RateLimiting.config.RateLimiterProperties;

import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RedisTokenBucketService {
    
    private final JedisPool jedisPool;;

    private final RateLimiterProperties properties;

    private final String TOKEN_KEY_PREFIX = "rate_limiter:tokens:";

    private static final String LAST_REFILL_KEY_PREFIX = "rate_limiter:last_refill:";

    public boolean isAllowed(String clinetId){

        String tokenKey = TOKEN_KEY_PREFIX + clinetId;

        try( Jedis jedis = jedisPool.getResource()){

            refillTokens(clinetId, jedis);

            String tokensStr = jedis.get(tokenKey);

            long currentTokens = tokensStr != null ? Long.parseLong(tokensStr) : properties.getCapacity();

            if(currentTokens < 0){
                return false;
            }

            long decremented = jedis.decr(tokenKey);
            return decremented >= 0;
        }
    }

    public long getCapacity(String clientId){
        return properties.getCapacity();
    }

    public long getAvailableTokens(String clientId){
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        try(Jedis jedis = jedisPool.getResource()){
            refillTokens(clientId, jedis);
            String tokensStr = jedis.get(tokenKey);
            return tokensStr != null ? Long.parseLong(tokensStr) : properties.getCapacity();
        }
    }

    public void refillTokens(String clientId, Jedis jedis){
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        String lastRefillKey = LAST_REFILL_KEY_PREFIX + clientId;

        long now = System.currentTimeMillis();
        String lastRefillStr = jedis.get(lastRefillKey);
        if(lastRefillStr == null){
            jedis.set(lastRefillKey, String.valueOf(now));
            jedis.set(tokenKey, String.valueOf(properties.getCapacity()));
            return;
        }

        long lastRefillTime = Long.parseLong(lastRefillStr);
        long elapsedTime = now - lastRefillTime;

        if(elapsedTime <= 0){
            return;
        }

        long tokensAdd = (elapsedTime * properties.getRefillRate()) / 1000;
        if(tokensAdd <= 0){
            return;
        }   

        String tokenStr = jedis.get(tokenKey);
        long currentTokens = tokenStr != null ? Long.parseLong(tokenStr) : properties.getCapacity();
        long newTokens = Math.min(properties.getCapacity(), currentTokens + tokensAdd);
        jedis.set(tokenKey, String.valueOf(newTokens));
        jedis.set(lastRefillKey, String.valueOf(now));
    }


}
