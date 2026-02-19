package com.example.RateLimiting.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTokenBucketService tokenBucketService;

    public boolean isAllowed(String clientId){
        return tokenBucketService.isAllowed(clientId);
    }

    public long getCapacity(String clientId){
        return tokenBucketService.getCapacity(clientId);
    }

    public long getAvailableTokens(String clientId){
        return tokenBucketService.getAvailableTokens(clientId);
    }
    
}
