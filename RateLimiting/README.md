# 🚀 Distributed API Gateway & Token Bucket Rate Limiter

A high-performance, horizontally scalable API Gateway built with **Spring Cloud Gateway** and **Redis**. It implements the **Token Bucket** rate-limiting algorithm using **Atomic Lua Scripting** to ensure strict consistency across distributed environments.

## 🛠️ Tech Stack
- **Framework:** Spring Boot 3.4.3 (Reactive Stack)
- **Gateway:** Spring Cloud Gateway 2024.0.0
- **Storage:** Redis (Jedis 5.2.0)
- **Concurrency:** Atomic Lua Scripting
- **Testing:** JUnit 5, Mockito, WireMock
- **DevOps:** Docker (Multi-stage builds)

## 🏗️ Architecture
```text
[ Client ] --(HTTP)--> [ Spring Cloud Gateway ] --(Proxy)--> [ Backend Services ]
                               |
                               | (Atomic Lua Execution)
                               v
                       [ Shared Redis Cluster ]
```

## ✨ Key Features
- **Atomic Operations:** Core rate-limiting logic is executed as a single transaction in Redis via Lua scripts, preventing race conditions in a distributed setup.
- **Lazy Refill Algorithm:** Tokens are refilled dynamically on-demand, reducing unnecessary background processing.
- **Customizable Quotas:** Easy configuration of capacity and refill rates per client/IP.
- **Header Injection:** Real-time feedback provided to clients via `X-RateLimit-Limit` and `X-RateLimit-Remaining` headers.
- **Health Monitoring:** Built-in endpoints for gateway health and real-time rate-limit status.

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Redis Server (Local or Cloud)
- Docker (Optional)

### 1. Configuration
Open `src/main/resources/application.properties` and update your Redis credentials:
```properties
spring.redis.host=your_redis_host
spring.redis.port=6379
spring.redis.password=your_password
```

### 2. Run Locally
```bash
./gradlew clean bootRun
```

### 3. Run with Docker
```bash
docker build -t rate-limiter-gateway .
docker run -p 8090:8090 -e REDIS_HOST=your_host rate-limiter-gateway
```

## 🧪 Testing the Rate Limiter

### Burst Test (Hit the Limit)
Run this loop to consume all tokens and see the rate-limit headers drop to zero:
```bash
for i in {1..11}; do curl -s -I http://localhost:8090/api/resource | grep X-RateLimit-Remaining; done
```

### Run Automated Tests
```bash
./gradlew test
```

## 📡 API Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/gateway/health` | Service health status |
| `GET` | `/gateway/rate-limit/status` | Current token bucket capacity and availability |
| `GET` | `/api/**` | Proxied requests (Rate Limited) |

---
*Developed as a demonstration of high-concurrency backend architecture and distributed systems.*
