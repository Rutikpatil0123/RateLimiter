package com.example.RateLimiting.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.RateLimiting.service.RateLimiterService;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.server.reactive.ServerHttpRequest;
import java.util.Map;



@RestController
@RequestMapping("/gateway")
public class StatusController {

    private final RateLimiterService rateLimiterService;

    public StatusController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Rate Limiter Gateway"
        )));
    }

    // Endpoint to check the rate limit status for a client

    @GetMapping("/rate-limit/status")
    public Mono<ResponseEntity<Map<String, Object>>> getRateLimiterStatus(ServerWebExchange exchange){
        String clinetId = getClientId(exchange);
        return Mono.just(ResponseEntity.ok(Map.of(
            "clientId", clinetId,
            "capacity", rateLimiterService.getCapacity(clinetId),
            "availableTokens", rateLimiterService.getAvailableTokens(clinetId)
        )));
    }

    // Helper method to extract client ID from request (e.g., from headers or query params)
    private String getClientId(ServerWebExchange exchange){
        ServerHttpRequest request = exchange.getRequest();
        String xForwrdedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if(xForwrdedFor != null && !xForwrdedFor.isEmpty()){
            return xForwrdedFor.split(",")[0].trim();
        }

        return "unknown-client";
    
}
}
