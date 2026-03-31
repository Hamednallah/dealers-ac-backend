# Database Design

## Tenancy Approach
The system uses **Database-Level Discriminator Columns** for performance and simplicity across a single PostgreSQL instance. All application entities explicitly bind `tenant_id` fields mapping to the header value authenticated per user request. 

### Core Tables

#### 1. `users`
- Stores authentication credentials with `BCrypt` hashes and role assignments.
- `tenant_id` determines organizational boundaries.

#### 2. `dealers`
- Represents a physical location.
- **Enums:** SubscriptionType (`BASIC`, `PREMIUM`). Enables API limits based on status.

#### 3. `vehicles`
- Linked intrinsically to a `dealer_id`.
- **Enums:** VehicleStatus (`AVAILABLE`, `SOLD`). 
- **Indexes:** Multi-column B-tree index on `(dealer_id, status)` for lightning-fast premium dashboard filtering.

#### 4. `audit_logs`
- Append-only transactional event log for tracking modification records across any table via AOP (Aspect-Oriented Programming).

#### 5. `jwt_blacklist`
- Stateless tokens cannot be mathematically revoked. This cache tracks explicitly logged-out `jti` structures locally.

> **Migration Engine:** Managed explicitly through `Flyway` V-scripts mapped directly inside `src/main/resources/db/migration`.
