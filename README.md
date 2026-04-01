# Dealers AC Backend System

![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)

A robust, production-grade Multi-Tenant Dealer and Vehicle Inventory Management Backend. Designed as a **Modular Monolith** using Clean Architecture principles to enforce strict domain boundaries while offering horizontal scalability.

## 📚 Documentation
Before diving into the code, please review the core design decisions:
- [System Architecture](docs/architecture_design.md) - Multi-tenancy strategy, isolation, and security.
- [Database Schema](docs/database_schema.md) - Entity relations and design considerations.

## ✨ Key Features
* **Multi-Tenant Isolation:** Secure `tenant_id` column discriminators bound to a ThreadLocal context wrapper filtering requests via the `X-Tenant-Id` header.
* **Stateless Security:** HS256 JWT Authentication with a local `jti` caching strategy for explicitly blacklisting revoked tokens.
* **Resilience & Rate Limiting:** IP-based Bucket4j rate limiting out-of-the-box.
* **Asynchronous Webhooks:** Spring Application Events decouple the standard request flow, allowing async Mailgun/Twilio dispatches for `VEHICLE_SOLD` triggers.
* **Automated CI/CD:** Fully functional GitHub Actions pipeline pushing verified code directly into Docker deployments.
* **Integration Testing:** Extensive `Testcontainers` lifecycle handling real PostgreSQL ephemeral databases.

## 🚀 Quick Start (Local Development)

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### 1. Configure the Environment
Copy the example environment securely:
```bash
cp .env.example .env
```
Update the `JWT_SECRET` inside `.env` with a secure base64 string.

### 2. Boot the Database Layer
Instead of installing PostgreSQL locally, leverage the internal container environment:
```bash
docker-compose up -d db
```
*Flyway migrations will automatically structure the schema on standard app startup.*

### 3. Start the Application
```bash
mvn spring-boot:run
```
Once initialized, traverse to the interactive API Documentation here:  
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

---

## 🏗️ Testing Strategy
Unit tests use Mockito for pure domain isolation, whilst Integration tests leverage **Testcontainers** to invoke Postgres containers validating end-to-end data lifecycle.

```bash
# Run all Unit and Integration Tests natively
mvn verify
```

## 🔒 Access Controls & RBAC
There are distinct role barriers enforced through Spring Security annotations:
* `ROLE_USER`: Standard tenant users limited specifically to their `X-Tenant-Id` domains.
* `ROLE_GLOBAL_ADMIN`: Superusers capable of pinging multi-tenant aggregating statistics (e.g. `/admin/dealers/countBySubscription`).

---
*Authored for the Dealers AC Technical Assessment.*
