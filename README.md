# Dealers AC Backend System

![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)

A robust, production-grade Multi-Tenant Dealer and Vehicle Inventory Management Backend.
Designed as a **Modular Monolith** using Clean Architecture principles that enforces strict domain boundaries while offering horizontal scalability.

---

## 📚 Documentation

Before diving into the code, review the core design decisions:

- [System Architecture](docs/architecture_design.md) — Multi-tenancy strategy, concurrency control, and security.
- [Database Schema](docs/database_schema.md) — Entity relations, ERD, and Flyway migration history.

---

## ✨ Key Features

- **Multi-Tenant Isolation:** Secure `tenant_id` column discriminators bound to a `ThreadLocal` context wrapper filtering requests via the `X-Tenant-Id` header.
- **Checkout Reservation Pattern:** `POST /vehicles/{id}/checkout` reserves a vehicle for **15 minutes** using JPA Optimistic Locking (`@Version`), atomically preventing two users from buying the same car simultaneously. Returns `409 Conflict` to the losing thread.
- **Reservation Sweeper:** A `@Scheduled` background job runs every 60 seconds to automatically release expired reservations back to `AVAILABLE`.
- **Stateless Security:** HS256 JWT Authentication with a `jti` blacklist strategy for explicitly revoking tokens.
- **Resilience & Rate Limiting:** IP-based Bucket4j token-bucket rate limiting out-of-the-box.
- **Asynchronous Event Bus:** Spring Application Events decouple the request flow, allowing async Mailgun/Twilio dispatches for `VEHICLE_SOLD` triggers.
- **Automated CI/CD:** GitHub Actions pipeline running tests and pushing verified builds.
- **Integration Testing:** Testcontainers lifecycle with real ephemeral PostgreSQL databases.

---

## 🚀 Quick Start (Local Development)

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### 1. Configure the Environment

```bash
cp .env.example .env
```

Update `JWT_SECRET` inside `.env` with a secure base64 string.

### 2. Boot the Database Layer

```bash
docker-compose up -d db
```

Flyway migrations run automatically on startup and structure the schema.

### 3. Start the Application

```bash
mvn spring-boot:run
```

API docs available at: 👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

---

## 🔄 Checkout API Flow

```
1. Client calls:  POST /vehicles/{id}/checkout
                  → Status: RESERVED_PENDING_PAYMENT
                  → reservation_expires_at = now + 15min

2. Client calls:  PATCH /vehicles/{id}  { "status": "SOLD" }
                  → Vehicle is sold, reservation cleared

3. If no action:  VehicleReservationSweeper reverts → AVAILABLE after 15min
```

---

## 🏗️ Testing Strategy

- **Unit tests:** Mockito for pure domain isolation (`mvn test`).
- **Integration tests:** Testcontainers with real PostgreSQL — requires Docker Desktop running.

```bash
# Run all unit tests (no Docker needed)
mvn test

# Run everything including integration tests (requires Docker)
mvn verify
```

---

## 🔒 Access Controls & RBAC

| Role | Access |
|------|--------|
| `TENANT_USER` | CRUD on dealers/vehicles scoped to their own `X-Tenant-Id` |
| `GLOBAL_ADMIN` | Cross-tenant aggregation: `GET /admin/dealers/countBySubscription` |

---

*Authored for the Dealers AC Technical Assessment.*
