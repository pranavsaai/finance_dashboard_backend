# Finance Dashboard Backend

A role-based finance backend built with Java 17, Spring Boot 3, and MongoDB. Handles financial records, enforces access control across three user roles, and serves aggregated dashboard analytics over a REST API.

---

## Table of Contents

- [The Problem I Was Solving](#the-problem-i-was-solving)
- [Key Design Decisions](#key-design-decisions)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Data Model](#data-model)
- [How a Request Flows Through the System](#how-a-request-flows-through-the-system)
- [Core Requirements — How Each Was Implemented](#core-requirements--how-each-was-implemented)
- [Optional Enhancements](#optional-enhancements)
- [Access Control Matrix](#access-control-matrix)
- [API Documentation](#api-documentation)
- [Setup and Running Locally](#setup-and-running-locally)
- [Environment Variables](#environment-variables)
- [Known Tradeoffs and Future Improvements](#known-tradeoffs-and-future-improvements)

---

## The Problem I Was Solving

Storing financial records is the easy part. The harder problem is: **who is allowed to see or change what — and how do you enforce that reliably, even across edge cases?**

Three roles interact with the system differently:

- A **Viewer** should only see the summary dashboard — never raw transaction records.
- An **Analyst** can read and filter records but cannot create, update, or delete anything.
- An **Admin** has full control — managing users, creating records, and modifying or removing them.

That sounds simple until you hit the edge case that shaped most of the architecture: **what if an admin is deactivated after they logged in?** Their JWT is still valid. A framework-level role check still passes them through. The system has to catch this at the business logic level — not just at the token level.

That one edge case is why access control here works the way it does.

---

## Key Design Decisions

### 1. Two independent access control layers

Most backends enforce roles only through annotations like `@PreAuthorize`. I added a second layer inside every service method via `resolveCaller()`.

The annotation layer reads the role from the JWT embedded at login time — fast, handles the common case. But it cannot catch a user who was deactivated after logging in, because it only reads what was encoded in the token at the time of issue.

`resolveCaller()` fixes this by hitting the database on every request — fetching the actual user, checking `isActive()`, and re-confirming the role with live data. If either layer rejects the request, it stops there. Both must pass independently.

This also means the system is safe even if the security filter layer were misconfigured — the service layer would still enforce access correctly.

### 2. MongoDB aggregations for the dashboard — not Java streams

The dashboard needs total income, total expenses, net balance, per-category breakdowns split by type, and monthly trends. Pulling all records into Java and computing with streams works at small scale — but breaks down as data grows, and pushes work the database is designed to do into application memory.

Every dashboard metric is computed inside MongoDB using aggregation pipelines. Java only receives the final result. The most complex pipeline is `getMonthlyTrends()`: it groups records by year and month in IST, then sums income as positive and expenses as negative using MongoDB's conditional operator — all in a single pipeline, no post-processing.

### 3. One dynamic filter query instead of many derived methods

The filter endpoint supports five optional parameters — type, category, date range, and keyword search — all fully combinable. The naive approach is to write a separate repository method for every possible combination (16+ permutations). That gets unmaintainable fast.

`filterDynamic()` in `FinancialRecordCustomRepositoryImpl` uses Spring Data's `Criteria` builder to construct a single query at runtime. It starts with an empty condition list, appends only the filters that were actually provided, and executes one query. Adding a new filter parameter later is a single `if` block — no new repository method needed.

### 4. Soft delete

Records are never physically removed. `DELETE /api/records/{id}` sets `deleted = true` and saves. Every query, aggregation, and dashboard pipeline filters `deleted: false` explicitly. The field is hidden from all API responses via `@JsonIgnore`.

This keeps the audit trail intact — the right default for any financial system. Callers never see the `deleted` field; to them, the record simply disappears.

### 5. Bootstrap admin creation

There's a chicken-and-egg problem in any role-based system: the first admin can't be created by an admin, because no admin exists yet. Rather than hardcoding credentials or shipping a seed script, `POST /api/users` stays publicly accessible only while the users collection is empty. The moment the first user exists, that endpoint requires an ADMIN token for all subsequent calls.

This is a single count check in `UserController` — no special configuration needed. `SecurityConfig` intentionally exposes only `POST /api/users` as public — not the entire `/api/users` route. All other methods on that path (`GET`, `PATCH`) require authentication. The first user is also validated to have `ADMIN` role — creating a Viewer or Analyst as the first account would lock you out of the system.

### 6. Input DTOs separate API contracts from persistence models

Controllers accept dedicated request DTOs, never raw entity objects. This prevents callers from injecting arbitrary state:

- `UserCreateRequest` accepts only `name`, `email`, `password`, and `role`. Fields like `id`, `active`, and `createdAt` are set internally by the service layer — absent from the DTO entirely.
- `UserUpdateRequest` accepts only the fields an admin may change on an existing user.
- `FinancialRecordRequest` carries the record payload. `userId` is stamped from the caller's JWT context at creation time — not accepted as a request field.

Response DTOs (`UserResponse`, `FinancialRecordResponse`) ensure internal fields like `password`, `userId`, and `deleted` are never surfaced in API responses.

### 7. Atomic rate limiting via `ConcurrentHashMap.compute()`

The rate limiter uses `compute()` rather than `getOrDefault` + `put` for atomic read-modify-write. The naive two-step approach has a race condition: two concurrent requests from the same IP can both read `count=0`, both increment to `1`, and both pass — effectively doubling the per-request budget. `compute()` eliminates this by making the entire check-and-increment one indivisible operation.

Stale entries are pruned probabilistically on ~0.1% of requests, keeping memory bounded without a dedicated cleanup thread.

Uses compute() for an atomic read-modify-write on the ConcurrentHashMap.
Avoids the race condition where two concurrent requests from the same IP both read count=0, both increment to 1, and both bypass the rate limit.
long[]: [0] = request count, [1] = window start time.


### 8. Password minimum length validation

`UserCreateRequest` validates `password` with both `@NotBlank` and `@Size(min = 8)`. A blank password is rejected by the former; a single-character password is rejected by the latter. Both cases return a field-level 400 with a clear message before any BCrypt operation runs.

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Database | MongoDB |
| Security | Spring Security + JWT (JJWT) |
| Password Hashing | BCrypt |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5 + Mockito + AssertJ |
| Build | Maven |
| API Docs | SpringDoc OpenAPI (Swagger UI) |

---

## Project Structure

```
com.zorvyn.finance/
│
├── config/
│   └── AppConfig.java                           # BCryptPasswordEncoder bean (isolated to prevent circular dependency)
│
├── controller/
│   ├── AuthController.java                      # POST /api/auth/login, /refresh
│   ├── UserController.java                      # User CRUD + role management
│   ├── FinancialRecordController.java            # Record CRUD + filter + pagination
│   └── DashboardController.java                 # Dashboard summary
│
├── service/
│   ├── UserService.java                         # User logic, resolveCaller(), bootstrap check
│   ├── FinancialRecordService.java              # Record logic, role assertions, pagination cap
│   └── DashboardService.java                   # Orchestrates aggregation calls
│
├── repository/
│   ├── UserRepository.java                      # MongoRepository for users
│   ├── FinancialRecordRepository.java           # MongoRepository + derived queries
│   ├── FinancialRecordCustomRepository.java     # Interface: aggregations + dynamic filter
│   └── FinancialRecordCustomRepositoryImpl.java # MongoTemplate: pipelines + Criteria builder
│
├── entity/
│   ├── User.java                                # users collection document
│   ├── FinancialRecord.java                     # records collection document
│   ├── Role.java                                # Enum: VIEWER, ANALYST, ADMIN
│   └── RecordType.java                          # Enum: INCOME, EXPENSE
│
├── dto/
│   ├── LoginRequest.java                        # Login payload
│   ├── RefreshRequest.java                      # Refresh token payload
│   ├── AuthResponse.java                        # Access + refresh token response
│   ├── UserCreateRequest.java                   # User creation payload (name, email, password, role only)
│   ├── UserResponse.java                        # Safe user response (no password field)
│   ├── UserUpdateRequest.java                   # Partial update: role, active, name, email
│   ├── FinancialRecordRequest.java              # Record creation/update payload
│   ├── FinancialRecordResponse.java             # Safe record response (no userId, no deleted)
│   ├── DashboardSummary.java                   # Aggregated dashboard response
│   └── PageResponse.java                        # Generic paginated response wrapper
│
├── security/
│   ├── AuthFilter.java                          # JWT validation + atomic per-IP rate limiting
│   ├── JwtUtil.java                             # Token generation and parsing
│   ├── AuthContext.java                         # ThreadLocal userId store (cleared after each request)
│   └── SecurityConfig.java                     # Spring Security configuration
│
├── exception/
│   ├── GlobalExceptionHandler.java             # @ControllerAdvice — maps all exceptions to HTTP responses
│   ├── AccessDeniedException.java              # 403
│   ├── UnauthorizedException.java              # 401
│   └── ResourceNotFoundException.java          # 404
│
└── FinanceApplication.java                      # Entry point + @EnableMongoAuditing
```

---

## Data Model

```
┌──────────────────────────────────────────┐
│                 users                    │
├──────────────────────────────────────────┤
│ _id         String (ObjectId)            │
│ name        String        [required]     │
│ email       String        [unique index] │
│ password    String        [bcrypt hash]  │
│ role        VIEWER | ANALYST | ADMIN     │
│ active      Boolean       [default: true]│
│ createdAt   LocalDateTime                │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│                records                   │
├──────────────────────────────────────────┤
│ _id         String (ObjectId)            │
│ amount      Double        [required, >0] │
│ type        INCOME | EXPENSE             │
│ category    String        [required]     │
│ date        LocalDateTime [required]     │
│ notes       String        [optional]     │
│ userId      String        → users._id    │
│ deleted     Boolean       [soft delete]  │
│ createdAt   LocalDateTime [auto-audited] │
│ updatedAt   LocalDateTime [auto-audited] │
└──────────────────────────────────────────┘
```

**Relationship:** One user → many records (via `userId` reference)

A few notes on modeling choices:

- `deleted` is always filtered in queries and hidden from responses via `@JsonIgnore` — callers never see it
- `createdAt` and `updatedAt` are populated automatically by `@EnableMongoAuditing` — no manual code needed
- `email` has a unique index at the MongoDB level via `@Indexed(unique = true)`, not just an application-level check
- `userId` on a record is stamped from the caller's JWT context at creation time — not accepted from the request body

---

## How a Request Flows Through the System

```
HTTP Request
     │
     ▼
AuthFilter
     ├── Check rate limit (100 req/IP/minute, atomic via compute()) → 429 if exceeded
     ├── Parse Authorization: Bearer <token>
     │       ├── Invalid/expired token → 401 immediately, request stops
     │       └── Valid token → extract userId + role
     ├── Set Spring SecurityContext (ROLE_ADMIN / ROLE_ANALYST / ROLE_VIEWER)
     └── Set AuthContext.set(userId) → ThreadLocal for service layer
     │
     ▼
@PreAuthorize (controller level)
     └── Role check against SecurityContext → 403 if role not allowed
     │
     ▼
Controller → Service
     └── resolveCaller()
             ├── Read userId from AuthContext ThreadLocal
             ├── Fetch actual User from MongoDB
             ├── Check user.isActive() → 401 if deactivated (catches stale token scenario)
             └── Return live User entity for role assertions
     │
     ▼
Business Logic + assertAdmin() / assertNotViewer()
     └── Second role check using live database user — not the token claim
     │
     ▼
Repository → MongoDB
     │
     ▼
Response returned via DTO (no internal fields exposed)
     │
     ▼
AuthContext.clear() ← always runs in finally block, prevents ThreadLocal leakage
```

---

## Core Requirements — How Each Was Implemented

### 1. User and Role Management

The role model is three-tiered: `VIEWER`, `ANALYST`, `ADMIN`. Roles are stored as strings in MongoDB and enforced at two independent layers on every request.

- **Create user** — `POST /api/users` accepts a `UserCreateRequest` DTO. Open only while the DB is empty (bootstrap). After that, an ADMIN token is required. `id`, `active`, and `createdAt` are set by the service — not suppliable by the caller.
- **Get all users** — `GET /api/users` — ADMIN only
- **Get user by ID** — `GET /api/users/{id}` — ADMIN only
- **Get own profile** — `GET /api/users/me` — any authenticated user
- **Update user** — `PATCH /api/users/{id}` — ADMIN only. Partial update — only non-null fields in the request body are applied. An admin cannot change their own role or deactivate their own account (would cause an immediate lockout).
- **User state** — deactivated users (`active: false`) are rejected in `resolveCaller()` before any business logic runs, even with a valid token
- **Passwords** — BCrypt-hashed on creation, minimum 8 characters enforced at the DTO level. Never stored in plain text. `UserResponse` always omits the password field.

### 2. Financial Records Management

Each record stores: `amount` (validated positive Double), `type` (INCOME/EXPENSE), `category`, `date`, `notes` (optional). `userId` is stamped from the caller's token on creation.

- **Create** — `POST /api/records` — ADMIN only
- **Read all** — `GET /api/records` — ANALYST + ADMIN
- **Read one** — `GET /api/records/{id}` — ANALYST + ADMIN
- **Update** — `PUT /api/records/{id}` — ADMIN only, full field update
- **Delete** — `DELETE /api/records/{id}` — ADMIN only, soft delete (sets `deleted: true`)
- **Filter** — `GET /api/records/filter` — five optional, fully combinable params via a single dynamic Criteria query
- **Paginated listing** — `GET /api/records/paginated` — page size validated and capped at 100

### 3. Dashboard Summary APIs

All metrics are computed inside MongoDB via aggregation pipelines. Java only receives final numbers.

| Field | How it's computed |
|---|---|
| `totalIncome` | Pipeline: match `type=INCOME`, sum `amount` |
| `totalExpense` | Pipeline: match `type=EXPENSE`, sum `amount` |
| `netBalance` | `totalIncome − totalExpense` (computed in Java after both aggregations complete) |
| `categoryTotals` | Pipeline: group by `category + type`, sum `amount` — kept split so INCOME and EXPENSE for the same category aren't merged into a misleading number |
| `monthlyTrends` | Pipeline: group by year+month in IST, net signed sum — income positive, expenses negative — single pipeline, no post-processing |
| `recentActivity` | Top 5 most recent non-deleted records, ordered by date descending via derived repository method |

### 4. Access Control Logic

Two layers, both must pass independently:

**Layer 1 — `@PreAuthorize` at controller**: Checks the role from the JWT in the Spring `SecurityContext`. Fast, handles the common case.

**Layer 2 — `resolveCaller()` + `assertAdmin()` / `assertNotViewer()` in services**: Fetches the actual user from the database on every request, checks both role and `isActive()`. This catches deactivated users that the annotation layer cannot catch, because `@PreAuthorize` only reads the role encoded in the token at login time. Even if the security filter were misconfigured, the service layer would still reject unauthorized access.

### 5. Validation and Error Handling

Bean validation on all request DTOs (`@NotNull`, `@NotBlank`, `@Positive`, `@Email`, `@Size`). A `GlobalExceptionHandler` maps every exception type to a consistent HTTP response:

| Exception | Status | Notes |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Returns field-level error map |
| `IllegalArgumentException` | 400 | e.g. `from` after `to` in date range, invalid page size |
| `HttpMessageNotReadableException` | 400 | Malformed JSON body |
| `ResourceNotFoundException` | 404 | |
| `UnauthorizedException` | 401 | Missing token, inactive account, bad credentials |
| `AccessDeniedException` (custom) | 403 | Service-layer role check failed |
| Spring's `AccessDeniedException` | 403 | `@PreAuthorize` check failed |
| `AuthenticationCredentialsNotFoundException` | 401 | No authentication provided at all |
| Anything else | 500 | Generic internal error, no details leaked |

### 6. Data Persistence

Two repository patterns used together via interface composition:

- **`MongoRepository`** — standard CRUD and derived query methods (`findByDeletedFalse`, `findTop5ByDeletedFalseOrderByDateDesc`, etc.)
- **`MongoTemplate` + Aggregation API** (`FinancialRecordCustomRepositoryImpl`) — all dashboard aggregation pipelines and the dynamic filter query

`FinancialRecordRepository` extends both interfaces, so service classes get a single clean dependency for all data access without having to manage two separate injections.

---

## Optional Enhancements

### JWT Authentication (Access + Refresh Tokens)

Login returns both an access token (15 min expiry) and a refresh token (7 days). The refresh endpoint validates the token type claim, checks that the user still exists and is active, and issues a new access token. A deactivated user cannot obtain new access tokens even with a still-valid refresh token — `getUserForRefresh()` checks `isActive()` 
explicitly.

Reject refresh tokens presented as Bearer tokens.
Previously, a refresh token would pass signature validation, extractRole() would return null, and the SecurityContext would be set with "ROLE_null" — wrong behavior even though endpoints would still reject it downstream via @PreAuthorize.

### Pagination

`GET /api/records/paginated?page=0&size=10` returns a typed `PageResponse<FinancialRecordResponse>` with `data`, `page`, `size`, and `total`. Records are sorted by date descending. Page index must be ≥ 0, size must be between 1 and 100 — anything outside returns a 400. The upper cap prevents clients from pulling the entire collection in one request.

### Search

`GET /api/records/filter?search=salary` runs a case-insensitive regex match across both `category` and `notes`. The `search` param is additive and fully combinable with type, category, and date range in the same dynamic query — no separate endpoint needed.

### Soft Delete

Records are never removed from the database. All queries and aggregations filter `deleted: false` explicitly. The field is invisible in every API response via `@JsonIgnore`.

### Rate Limiting

Each IP is limited to 100 requests per 60-second window, implemented inside `AuthFilter`. The increment uses `ConcurrentHashMap.compute()` for atomic read-modify-write — this avoids the race condition where two concurrent requests from the same IP both read the same stale count and both pass. Stale entries are pruned on ~0.1% of requests to keep memory bounded. Exceeded requests return 429 with a JSON error body.

### Unit Tests

Service-layer tests with JUnit 5, Mockito, and AssertJ:

- `FinancialRecordServiceTest` — 20 tests covering create/read/delete blocked by role, soft delete verification, filter combinations, pagination bounds, keyword search
- `UserServiceTest` — 9 tests covering `resolveCaller` with null token / inactive user / active user, role-based access, partial update logic, duplicate email handling, `UserCreateRequest` DTO mapping

Every test calls `AuthContext.clear()` in `@AfterEach` to prevent ThreadLocal state from leaking across tests. All 29 tests pass.

### API Documentation

Swagger UI is auto-configured via SpringDoc OpenAPI and available at `http://localhost:8080/swagger-ui/index.html` once the server is running.

---

## Access Control Matrix

| Action | VIEWER | ANALYST | ADMIN |
|---|---|---|---|
| Login / Refresh token | ✓ | ✓ | ✓ |
| View dashboard summary | ✓ | ✓ | ✓ |
| View own profile (`/me`) | ✓ | ✓ | ✓ |
| View / filter records | ✗ | ✓ | ✓ |
| View record by ID | ✗ | ✓ | ✓ |
| Paginated records | ✗ | ✓ | ✓ |
| Create records | ✗ | ✗ | ✓ |
| Update records | ✗ | ✗ | ✓ |
| Delete records | ✗ | ✗ | ✓ |
| Manage users | ✗ | ✗ | ✓ |

---

## API Documentation

All protected endpoints require:
```
Authorization: Bearer <access_token>
```

---

### Auth

#### `POST /api/auth/login`

```json
{
  "email": "admin@example.com",
  "password": "secret123"
}
```

**Response 200:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci..."
}
```

**Errors:** `401` invalid credentials or inactive account

---

#### `POST /api/auth/refresh`

```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Response 200:** New `accessToken`, same `refreshToken`

**Errors:** `401` if token is invalid, expired, not a refresh token, or the user has been deactivated since the refresh token was issued

---

### Users

#### `POST /api/users`
No auth required for the first user only. ADMIN token required after that.

```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "pass1234",
  "role": "ANALYST"
}
```

**Response 201:**
```json
{
  "id": "661a3f...",
  "name": "Alice",
  "email": "alice@example.com",
  "role": "ANALYST",
  "active": true,
  "createdAt": "2025-01-15T10:00:00"
}
```

**Errors:** `400` validation errors, duplicate email, or first user is not ADMIN | `403` non-admin after bootstrap

---

#### `GET /api/users` — ADMIN only
Returns all users. Password field is never included.

#### `GET /api/users/{id}` — ADMIN only
**Errors:** `404` user not found

#### `GET /api/users/me` — Any authenticated user
Returns the currently logged-in user's profile.

#### `PATCH /api/users/{id}` — ADMIN only
Partial update — only the fields you send are changed.

```json
{
  "role": "VIEWER",
  "active": false
}
```

**Errors:** `400` if admin tries to change their own role or deactivate themselves

---

### Financial Records

#### `POST /api/records` — ADMIN only

```json
{
  "amount": 75000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-01-15T00:00:00",
  "notes": "January salary"
}
```

**Response 201:**
```json
{
  "id": "rec-abc123",
  "amount": 75000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-01-15T00:00:00",
  "notes": "January salary",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

**Errors:** `400` validation | `403` non-admin

---

#### `GET /api/records` — ANALYST + ADMIN
All non-deleted records.

#### `GET /api/records/{id}` — ANALYST + ADMIN
**Errors:** `404` not found | `403` viewer

#### `PUT /api/records/{id}` — ADMIN only
Full field update. Returns the updated record.
**Errors:** `404` not found | `403` non-admin

#### `DELETE /api/records/{id}` — ADMIN only
Soft delete. **Response 204** — no content. Record stays in the database with `deleted: true`.

---

#### `GET /api/records/filter` — ANALYST + ADMIN

All five params are optional and fully combinable:

| Param | Type | Description |
|---|---|---|
| `type` | `INCOME` or `EXPENSE` | Filter by record type |
| `category` | String | Exact category match |
| `from` | `YYYY-MM-DD` | Start of date range |
| `to` | `YYYY-MM-DD` | End of date range |
| `search` | String | Case-insensitive search across category + notes |

```
GET /api/records/filter?type=INCOME
GET /api/records/filter?category=Salary&from=2025-01-01&to=2025-01-31
GET /api/records/filter?search=electricity&type=EXPENSE
GET /api/records/filter?from=2025-01-01&to=2025-03-31
```

**Errors:** `400` if only one of `from`/`to` is provided, or if `from` is after `to`

---

#### `GET /api/records/paginated` — ANALYST + ADMIN

| Param | Default | Constraint |
|---|---|---|
| `page` | `0` | Must be ≥ 0 |
| `size` | `10` | Must be between 1 and 100 |

**Response 200:**
```json
{
  "data": [ ... ],
  "page": 0,
  "size": 10,
  "total": 42
}
```

---

### Dashboard

#### `GET /api/dashboard/summary` — All roles

**Response 200:**
```json
{
  "totalIncome": 170000.0,
  "totalExpense": 45000.0,
  "netBalance": 125000.0,
  "categoryTotals": {
    "Salary":    { "INCOME": 150000.0 },
    "Freelance": { "INCOME": 20000.0 },
    "Utilities": { "EXPENSE": 12000.0 },
    "Rent":      { "EXPENSE": 33000.0 }
  },
  "recentActivity": [
    {
      "id": "rec-abc123",
      "type": "INCOME",
      "amount": 75000.0,
      "category": "Salary",
      "date": "2025-02-15T00:00:00"
    }
  ],
  "monthlyTrends": {
    "2025-01": 55000.0,
    "2025-02": -12000.0
  }
}
```

`categoryTotals` is split by type — INCOME and EXPENSE for the same category are never merged into a single number.

`monthlyTrends` values are net per month — positive means net income, negative means net expense for that month.

---

## Setup and Running Locally

### Prerequisites
- Java 17+
- Maven 3.8+
- MongoDB running on `localhost:27017` (or a remote/Atlas URI)

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/pranavsaai/finance-backend.git
cd finance-backend

# 2. Set environment variables
export MONGO_URI_FINANCE=mongodb://localhost:27017/finance_db
export JWT_SECRET_FINANCE=$(openssl rand -base64 32)

# 3. Run
mvn spring-boot:run
```

Server starts at `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Running Tests

```bash
mvn test
```

The Spring context test (`FinanceApplicationTests`) requires a MongoDB instance at `mongodb://localhost:27017/finance_test`.

---

## Environment Variables

Copy `.env.example` to `.env` and fill in your values. Never commit `.env`.

| Variable | Description |
|---|---|
| `MONGO_URI_FINANCE` | MongoDB connection string |
| `JWT_SECRET_FINANCE` | JWT signing secret — must be a Base64-encoded string. Generate with `openssl rand -base64 32` |

Access tokens expire in **15 minutes**. Refresh tokens expire in **7 days**.

---

## Known Tradeoffs and Future Improvements

### Rate limiting is in-memory only

The rate limiter uses a `ConcurrentHashMap` inside `AuthFilter` — works for a single instance, resets on restart, and won't coordinate across multiple nodes.

**Production approach:** Distributed rate limiter backed by Redis using a sliding window or token bucket algorithm (e.g. Bucket4j + Redis).

### Bootstrap endpoint stays open at the HTTP layer

`POST /api/users` is permitted in `SecurityConfig` so the first admin can be created without a token. After bootstrap, an application-level guard in `UserController` enforces ADMIN-only access. Only `POST` is exposed this way — all other methods on `/api/users` require authentication.

**Production approach:** Automatically close the public route after the first user is created, or use an invite/onboarding flow.

### Search is regex-based, not index-backed

The `search` parameter runs a case-insensitive regex across `category` and `notes`. Correct, but regex queries aren't index-backed in MongoDB by default — they'll slow down at large data volumes.

**Production approach:** MongoDB text index on those fields, or Atlas Search for full-text capabilities.

### Soft delete without archival

Deleted records stay in the `records` collection permanently. At scale, this keeps growing the collection even though those records are never surfaced.

**Production approach:** Move soft-deleted records to a separate archive collection, or apply a TTL index to clean them up after a defined retention period.

### Dual-layer security means role logic lives in two places

`@PreAuthorize` and `resolveCaller()` both check roles. This is intentional defense-in-depth — but if role names ever change, both layers need updating.

**Production approach:** Consolidate into a centralized AOP-based authorization advice that handles role checks and active status in one place.

### No controller-level tests

Unit tests cover the service layer where business logic and access control rules live. Controller tests weren't included to keep the setup straightforward — they'd require full JWT generation and seeded users to run.

**Future improvement:** MockMvc or Testcontainers to validate the full request/response cycle including auth headers, HTTP status codes, and response shapes.

### Timezone is hardcoded to IST

`getMonthlyTrends()` groups records using `Asia/Kolkata` as the timezone. Appropriate for the current context, but would need to be configurable for multi-region deployment.

**Future improvement:** Expose timezone as a configurable property in `application.properties`.