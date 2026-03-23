package com.example.RateLimiting;

import com.example.RateLimiting.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Full Workflow Integration Test for the Rate Limiter Gateway.
 * 
 * Flow: WebTestClient (Request) -> Gateway -> RateLimiterFilter -> WireMock
 * (Target)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "rate-limiter.api-server-url=http://localhost:${wiremock.server.port}",
        "spring.data.redis.host=localhost", // Dummy
        "spring.data.redis.port=6379"
})
class TokenBucketIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean // Replaces the real RateLimiterService in the context
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void resetState() {
        // WireMock is global, so we must reset it between test methods
        WireMock.reset();
    }

    @Test
    @DisplayName("Workflow: Allowed request should be routed to upstream and include rate limit headers")
    void allowedRequest_routedToUpstream() {
        // 1. Mock service behavior
        when(rateLimiterService.isAllowed(anyString())).thenReturn(true);
        when(rateLimiterService.getCapacity(anyString())).thenReturn(10L);
        when(rateLimiterService.getAvailableTokens(anyString())).thenReturn(9L);

        // 2. Mock upstream response
        stubFor(get(urlEqualTo("/resource"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello from Backend!")));

        // 3. Perform request
        webClient.get().uri("/api/resource")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-RateLimit-Limit", "10")
                .expectHeader().valueEquals("X-RateLimit-Remaining", "9")
                .expectBody(String.class).isEqualTo("Hello from Backend!");

        // Verify upstream was actually hit
        WireMock.verify(1, getRequestedFor(urlEqualTo("/resource")));
    }

    @Test
    @DisplayName("Workflow: Blocked request should return 429 and skip upstream")
    void blockedRequest_returns429() {
        // 1. Mock service behavior to BLOCK
        when(rateLimiterService.isAllowed(anyString())).thenReturn(false);
        when(rateLimiterService.getCapacity(anyString())).thenReturn(10L);
        when(rateLimiterService.getAvailableTokens(anyString())).thenReturn(0L);

        // 2. Perform request
        webClient.get().uri("/api/resource")
                .header("X-Forwarded-For", "1.2.3.4")
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectHeader().valueEquals("X-RateLimit-Limit", "10")
                .expectHeader().valueEquals("X-RateLimit-Remaining", "0")
                .expectBody()
                .jsonPath("$.error").isEqualTo("Rate limit exceeded. Try again later.")
                .jsonPath("$.clientId").isEqualTo("1.2.3.4");

        // 3. Verify WireMock was NEVER called in THIS test
        WireMock.verify(0, getRequestedFor(urlPathMatching(".*")));
    }

    @Test
    @DisplayName("Status Endpoint: Should return current rate limit state")
    void gatewayStatus_returnsRateLimitInfo() {
        when(rateLimiterService.getCapacity(anyString())).thenReturn(100L);
        when(rateLimiterService.getAvailableTokens(anyString())).thenReturn(42L);

        webClient.get().uri("/gateway/rate-limit/status")
                .header("X-Forwarded-For", "9.9.9.9")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.clientId").isEqualTo("9.9.9.9")
                .jsonPath("$.capacity").isEqualTo(100)
                .jsonPath("$.availableTokens").isEqualTo(42);

        // Also verify hit count
        WireMock.verify(0, getRequestedFor(urlPathMatching(".*")));
    }
}
