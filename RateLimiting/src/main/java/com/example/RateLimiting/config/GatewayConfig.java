package com.example.RateLimiting.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.RateLimiting.filter.TokenBucketRateLimiterFilter;


//Defined how spring cloud gateway will route requests to our API server and apply rate limiting filter

@Configuration
public class GatewayConfig {

    private final TokenBucketRateLimiterFilter tokenBucketRateLimiterFilter;
    private final RateLimiterProperties rateLimiterProperties;

    public GatewayConfig(TokenBucketRateLimiterFilter tokenBucketRateLimiterFilter, RateLimiterProperties rateLimiterProperties) {
        this.tokenBucketRateLimiterFilter = tokenBucketRateLimiterFilter;
        this.rateLimiterProperties = rateLimiterProperties;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("api_route", r -> r.path("/api/**")
                        .filters(f -> f
                            .stripPrefix(1)
                            .filter(tokenBucketRateLimiterFilter.apply(new TokenBucketRateLimiterFilter.Config())))
                        .uri(rateLimiterProperties.getApiServerUrl()))
                .build();
    }
    
}
