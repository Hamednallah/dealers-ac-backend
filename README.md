# Dealers AC — Vehicle Inventory Backend

> **Back End Developer Take-Home Task** | Ahmed Abdulafiz
>
> A production-quality **multi-tenant Dealer & Vehicle Inventory Module** built as a **Modular Monolith** using Spring Boot 3, Spring Security 6, JWT, and PostgreSQL.

---

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+, Maven 3.9+

### 1. Clone and configure
```bash
cp .env.example .env
# Edit .env — set JWT_SECRET (min 32 chars) and optionally MAILGUN_* / WEBHOOK_URL
```

### 2. Start with Docker Compose
```bash
docker-compose up -d
# App: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### 3. Or run locally (requires PostgreSQL running)
```bash
# Start only PostgreSQL
docker-compose up -d postgres

# Run the app
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Default admin credentials
```
username: admin
password: Admin@1234!   (or set ADMIN_PASSWORD env var)
```

---

## Architecture

### Modular Monolith with Clean Architecture

Each module enforces strict separation of responsibilities:

```
dealers-ac-backend/
├── common/               ← Shared infrastructure (security, tenant, audit, webhooks)
│   ├── config/           ← SecurityConfig, OpenApiConfig, AppConfig
│   ├── security/         ← JwtUtil, JwtAuthFilter
│   ├── tenant/           ← TenantContext (ThreadLocal), TenantFilter
│   ├── audit/            ← AuditableEntity, AuditLog, @Audited AOP aspect
│   ├── ratelimit/        ← Bucket4j RateLimitFilter
│   └── webhook/          ← DomainEvent, WebhookDispatcher (async)
│
├── auth/                 ← Register, Login, JWT issuance
├── dealer/               ← Dealer CRUD + PDF inventory report
├── vehicle/              ← Vehicle CRUD + dynamic JPA Specification filters
├── admin/                ← Global admin stats (GLOBAL_ADMIN only)
└── integration/          ← Mailgun, Twilio stub, 700Credit stub
```

### Multi-Tenancy

```
Request ──►  TenantFilter ──►  JwtAuthFilter ──►  Controller ──►  Service
              │                    │
              │ Sets TenantContext  │ Validates JWT tenantId
              │ from X-Tenant-Id   │ matches X-Tenant-Id header
              ▼                    ▼
         ThreadLocal           403 if mismatch
              │
              ▼
         All service methods check: entity.tenantId == TenantContext.get()
         Cross-tenant → 404 (IDOR protection)
```

---

## Endpoints

### Auth (no tenant header required)
| Method | Path | Description |
|---|---|---|
| POST | `/auth/register` | Register a new `TENANT_USER`, returns JWT |
| POST | `/auth/login` | Login, returns JWT (rate-limited: 5/min/IP) |

### Dealers (requires `X-Tenant-Id` + JWT)
| Method | Path | Description |
|---|---|---|
| POST | `/dealers` | Create dealer |
| GET | `/dealers/{id}` | Get dealer by ID |
| GET | `/dealers?page=&sort=` | List all dealers (paged + sorted) |
| PATCH | `/dealers/{id}` | Partial update |
| DELETE | `/dealers/{id}` | Delete |
| GET | `/dealers/{id}/report` | Download PDF inventory report |

### Vehicles (requires `X-Tenant-Id` + JWT)
| Method | Path | Description |
|---|---|---|
| POST | `/vehicles` | Create vehicle (validates dealer belongs to same tenant) |
| GET | `/vehicles/{id}` | Get vehicle by ID |
| GET | `/vehicles?model=&status=&priceMin=&priceMax=&subscription=&page=&sort=` | Filter + paged |
| PATCH | `/vehicles/{id}` | Partial update (SOLD status fires email + webhook) |
| DELETE | `/vehicles/{id}` | Delete |

### Admin (requires `GLOBAL_ADMIN` JWT — no tenant header needed)
| Method | Path | Description |
|---|---|---|
| GET | `/admin/dealers/countBySubscription` | Count dealers by subscription (global) |

---

## Key HTTP Headers

| Header | Required | Description |
|---|---|---|
| `Authorization: Bearer <JWT>` | All protected routes | JWT from login/register |
| `X-Tenant-Id: <tenantId>` | All routes except `/auth/**` and `/admin/**` | Tenant identifier |

---

## Design Decisions

### 1. Admin Count is GLOBAL, not per-tenant
`GET /admin/dealers/countBySubscription` returns counts across **all tenants combined**.

**Reasoning:** `GLOBAL_ADMIN` has no tenant affiliation — their role is operational oversight across the entire system. A per-tenant count would require a tenant header, which contradicts the admin's global scope. The response `{ "BASIC": 12, "PREMIUM": 5 }` represents all dealers across the platform.

### 2. Cross-tenant access returns 404, not 403
When a TENANT_USER accesses a resource belonging to another tenant, the system returns **404 Not Found** instead of 403 Forbidden. This is a deliberate security decision — returning 403 would confirm that the resource *exists*, leaking information to a potential attacker. 404 reveals nothing.

### 3. Manual tenant enforcement vs. Hibernate @Filter
Chosen **manual service-layer checks** (`entity.tenantId == TenantContext.get()`). This is:
- **Explicit** — every cross-tenant violation is a visible code path
- **Testable** — easy to unit test each service method
- **Auditable** — a security reviewer can trace every enforcement point

### 4. ThreadLocal for tenant context
`TenantContext` uses `ThreadLocal<String>` cleared in a `finally` block after each request. This is:
- Thread-safe (one value per request thread)
- Framework-agnostic
- Zero-allocation read path

### 5. JPA Specification for dynamic vehicle filters
`VehicleSpecification` uses `JpaSpecificationExecutor` to compose predicates at runtime. This avoids an explosion of query method combinations and makes each filter independently testable.

### 6. VEHICLE_SOLD event is async
When a vehicle status changes to SOLD, a `DomainEvent` is published via Spring's `ApplicationEventPublisher`. The Mailgun email and webhook dispatch run `@Async` on a dedicated thread pool. This keeps the API response fast and decoupled from external HTTP calls.

---

## Security

| Layer | Control |
|---|---|
| **JWT** | HS256, env-var secret, 15min expiry, `jti` for revocation |
| **Password** | BCrypt strength 12 |
| **Rate Limiting** | 5 req/min on login, 10 on register (Bucket4j) |
| **Tenant Isolation** | Header + JWT claim match check, service-layer entity validation |
| **IDOR Prevention** | Cross-tenant returns 404 |
| **Method Security** | `@PreAuthorize` + URL-level matcher (defense in depth) |
| **Security Headers** | CSP, HSTS, X-Frame-Options: DENY, nosniff, Referrer-Policy |
| **Input Validation** | Bean Validation on all DTOs, fail on unknown enum values |
| **PII Protection** | Email excluded from logs, `@ToString(exclude = "email")` |
| **Error Leakage** | RFC 7807 ProblemDetail, no stack traces, generic messages |

---

## Integrations Demonstrated

| Integration | Status | Description |
|---|---|---|
| **Mailgun** | Configurable (env vars) | Email sent on VEHICLE_SOLD event via HTTP API |
| **Webhooks** | Configurable (env vars) | Async POST to webhook URL with exponential backoff |
| **Twilio** | Stub (interface documented) | SMS notification — shows SDK integration pattern |
| **700Credit** | Stub (interface documented) | Credit check — shows HTTP call pattern |

To enable Mailgun, set in `.env`:
```
MAILGUN_ENABLED=true
MAILGUN_API_KEY=key-xxxxx
MAILGUN_DOMAIN=mg.yourdomain.com
```

---

## Running Tests

```bash
# Unit tests only (no Docker needed)
mvn test

# Full suite including integration tests (Testcontainers starts PostgreSQL automatically)
mvn verify

# Integration tests only
mvn verify -Dtest=*IT
```

### What the integration tests cover
- ✅ Missing `X-Tenant-Id` → **400 Bad Request**
- ✅ Cross-tenant dealer access → **404 Not Found**
- ✅ Own-tenant dealer access → **200 OK**
- ✅ `TENANT_USER` on admin endpoint → **403 Forbidden**
- ✅ `GLOBAL_ADMIN` on admin endpoint → **200 OK** with correct counts
- ✅ No JWT → **403 Forbidden**

---

## Cloud Deployment

### Azure App Service
```bash
# Build image
docker build -t dealersac-backend .

# Tag and push to Azure Container Registry
az acr build --registry <your-acr> --image dealersac-backend:latest .

# Deploy
az webapp create --resource-group <rg> --plan <plan> \
  --name dealersac-api --deployment-container-image-name <acr>/dealersac-backend:latest
```

Configure env vars in Azure Portal → App Service → Configuration → Application Settings.

### AWS Elastic Beanstalk
```bash
# Package
mvn clean package -DskipTests

# Deploy .jar to Elastic Beanstalk via AWS CLI or EB CLI
eb init && eb create && eb setenv JWT_SECRET=<secret> DB_URL=<rds-url> ...
eb deploy
```

### Health & Metrics
- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- **Azure Application Insights** compatible via Micrometer (add `applicationinsights-spring-boot-starter` dependency)

---

## Project Structure Summary

```
src/main/java/com/dealersac/inventory/
├── DealersAcApplication.java
├── common/          ← Security, tenant, audit, rate limiting, webhooks
├── auth/            ← JWT auth (register, login)
├── dealer/          ← Dealer CRUD + PDF report
├── vehicle/         ← Vehicle CRUD + JPA Specification filters
├── admin/           ← Global admin stats
└── integration/     ← Mailgun, Twilio, 700Credit

src/main/resources/
├── application.yml
├── application-dev.yml
└── db/migration/    ← Flyway V1–V5

src/test/java/       ← Unit (Mockito) + Integration (Testcontainers)
```
