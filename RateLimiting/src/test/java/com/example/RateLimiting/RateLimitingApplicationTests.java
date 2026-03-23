package com.example.RateLimiting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Context load smoke-test.
 * Overrides Redis host so the context doesn't try to reach a live server.
 * The JedisPool bean is initialised lazily; the gateway never actually
 * connects during bean wiring, so the context comes up fine.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"spring.data.redis.host=localhost",
		"spring.data.redis.port=6379",
		"spring.data.redis.password=",
		"spring.redis.host=localhost",
		"spring.redis.port=6379",
		"rate-limiter.api-server-url=http://localhost:9999"
})
class RateLimitingApplicationTests {

	@Test
	void contextLoads() {
		// Verifies the Spring application context starts without errors.
	}
}
