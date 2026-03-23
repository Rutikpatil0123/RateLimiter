package com.example.RateLimiting.filter;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import com.example.RateLimiting.service.RateLimiterService;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;

@Component
public class TokenBucketRateLimiterFilter extends AbstractGatewayFilterFactory<TokenBucketRateLimiterFilter.Config> {

    private final RateLimiterService rateLimiterService;

    public TokenBucketRateLimiterFilter(RateLimiterService rateLimiterService) {
        super(Config.class);
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public TokenBucketRateLimiterFilter.Config newConfig() {
        return new Config();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // Handle CORS Preflight
            if (request.getMethod().name().equals("OPTIONS")) {
                return chain.filter(exchange);
            }

            String clientId = getClientId(request);

            if (!rateLimiterService.isAllowed(clientId)) {
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                addRateLimitHeaders(response, clientId);

                String errorBody = String.format(
                        "{\"error\": \"Rate limit exceeded. Try again later.\", \"clientId\": \"%s\"}", clientId);
                return response.writeWith(
                        Mono.just(response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8))));
            }

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                addRateLimitHeaders(response, clientId);
            }));
        };
    }

    private void addRateLimitHeaders(ServerHttpResponse response, String clientId) {
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(rateLimiterService.getCapacity(clientId)));
        response.getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(rateLimiterService.getAvailableTokens(clientId)));
        // Expose headers to the browser
        response.getHeaders().add("Access-Control-Expose-Headers", "X-RateLimit-Limit, X-RateLimit-Remaining");
    }

    public String getClientId(ServerHttpRequest request) {

        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // Fallback to direct connection IP if X-Forwarded-For is not present
        var remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    public static class Config {
        // Add any configuration properties if needed
    }

}
