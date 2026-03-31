# System Architecture Design

## 1. Overview
The Dealers Auto Center (AC) system is designed as a modular monolith. This approach provides the simplicity of a single deployment unit while enforcing strict boundaries between domains (Auth, Dealer, Vehicle, Admin) to ensure the system is ready to be split into microservices in the future if scale demands.

## 2. Multi-Tenancy Strategy
To securely isolate data between different client networks, we chose a **Discriminator Column** (Single Database, Single Schema) multi-tenant architecture. 
- **Header Propagation:** `X-Tenant-Id` is required on all protected routes.
- **Tenant Context:** A `ThreadLocal` context filter securely captures the tenant per request.
- **Data Isolation:** All entities implement IDOR protection. Every query explicitly checks `tenant_id` alongside the primary key to ensure data leakage is mathematically impossible.

## 3. Security
- **Stateless Authentication:** JSON Web Tokens (JWT) signed with HS256 algorithm.
- **Rate Limiting:** IP-based bucket rate limiting using Bucket4j to prevent brute-force attacks.
- **Role-Based Access Control (RBAC):** Global Admins vs Tenant standard users explicitly mapped to endpoint authorities.

## 4. Asynchronous Integrations
Instead of synchronous blocking calls, the system leverages Spring Cloud/Application Events to decouple domain logic.
- When a Vehicle status changes to `SOLD`, an event is emitted.
- Listeners catch this event to asynchronously trigger Mailgun emails, Webhooks, or Twilio notifications, ensuring the main thread responds to the user instantly.
