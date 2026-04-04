# Finance Dashboard Backend

A role-based finance backend that manages financial records, enforces access control across three user roles, and serves aggregated dashboard analytics — built as a REST API using Spring Boot and MongoDB.

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

The core challenge in this system isn't storing financial records — that's straightforward. The real problem is **who can do what, and how do you enforce it reliably**.

Three types of users interact with the system differently:
- A **Viewer** should only see summary-level dashboard data — never the raw records.
- An **Analyst** can read and filter records but cannot create or modify anything.
- An **Admin** has full control — managing users, creating records, updating and deleting them.

Beyond role enforcement, there's a second layer of complexity: a user might have the right role but be **deactivated**. A deactivated admin should not be able to act even if they hold a valid JWT token. The system needs to catch this at the business logic level, not just at the token level.

These two problems — role enforcement and live user state validation — shaped most of the architectural decisions described below.

---

## Key Design Decisions

### 1. Dual-Layer Access Control (not just annotations)

Most backends enforce roles only via framework annotations (`@PreAuthorize`). I deliberately added a **second enforcement layer inside every service method** via `resolveCaller()`.

Here's why: `@PreAuthorize` only checks the role embedded in the JWT at login time. If a user is deactivated *after* they logged in, their token is still valid. The annotation layer would let them through. The service layer catches this — it fetches the actual user from the database on every request and checks `isActive()` before proceeding.

This means even if the security filter were bypassed or misconfigured, no unauthorized action would succeed. Both layers must pass independently.

### 2. MongoDB Aggregation for Dashboard (not Java streams)

The dashboard needs total income, total expenses, net balance, per-category totals, and monthly trends. I could have fetched all records and calculated these in Java — but that doesn't scale. At large data volumes, loading thousands of records into memory just to sum them is wasteful.

Instead, each dashboard metric is computed by a **MongoDB aggregation pipeline** that runs entirely inside the database. Java only receives the final result. The monthly trends pipeline is the most involved — it groups records by year+month, then sums income as positive and expenses as negative using a conditional operator inside MongoDB, all with IST timezone awareness.

### 3. Dynamic Filter Query (Criteria Builder, not derived methods)

Filtering records supports five optional parameters: type, category, date range, and keyword search — all combinable. The naive approach is to write a separate repository method for every combination, which gives you 8+ methods that grow exponentially with each new parameter.

Instead, I built a single `filterDynamic()` method using MongoDB's `Criteria` builder. It starts with an empty criteria list, appends only the conditions that are actually present, and runs one query. Adding a new filter parameter in the future is one `if` block — not a new method.

### 4. Soft Delete

Records are never physically removed. `DELETE /api/records/{id}` sets `deleted = true` and saves. Every query, aggregation, and dashboard calculation explicitly filters `deleted: false`. The `deleted` field is hidden from all API responses via `@JsonIgnore`.

This preserves audit history, allows recovery if something is deleted by mistake, and is the correct behavior for financial systems.

### 5. Bootstrap User Creation

There's a chicken-and-egg problem in any access-controlled system: the first admin user can't be created by an admin because no admin exists yet. Rather than hardcoding credentials or shipping a seed script, I made `POST /api/users` publicly accessible **only when the database is empty**. The moment the first user is created, that endpoint requires an ADMIN token for all subsequent calls. This is handled cleanly in `UserController` without any special config.

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

---

## Project Structure

```
com.zorvyn.finance/
│
├── config/
│   └── AppConfig.java                       # BCryptPasswordEncoder bean (isolated to prevent circular dependency)
│
├── controller/
│   ├── AuthController.java                  # POST /api/auth/login, /refresh
│   ├── UserController.java                  # User CRUD + role management
│   ├── FinancialRecordController.java        # Record CRUD + filter + pagination
│   └── DashboardController.java             # Dashboard summary
│
├── service/
│   ├── UserService.java                     # User logic, resolveCaller(), bootstrap check
│   ├── FinancialRecordService.java          # Record logic, role assertions
│   └── DashboardService.java               # Orchestrates aggregation calls
│
├── repository/
│   ├── UserRepository.java                  # MongoRepository for users
│   ├── FinancialRecordRepository.java       # MongoRepository + derived queries
│   ├── FinancialRecordCustomRepository.java # Interface: aggregations + dynamic filter
│   └── FinancialRecordCustomRepositoryImpl.java  # MongoTemplate: pipelines + Criteria builder
│
├── entity/
│   ├── User.java                            # users collection document
│   ├── FinancialRecord.java                 # records collection document
│   ├── Role.java                            # Enum: VIEWER, ANALYST, ADMIN
│   └── RecordType.java                      # Enum: INCOME, EXPENSE
│
├── dto/
│   ├── LoginRequest.java                    # Login payload
│   ├── RefreshRequest.java                  # Refresh token payload
│   ├── AuthResponse.java                    # Access + refresh token response
│   ├── UserResponse.java                    # Safe user response (no password)
│   ├── UserUpdateRequest.java               # Partial update: role and/or active
│   ├── DashboardSummary.java               # Aggregated dashboard response
│   └── PageResponse.java                   # Generic paginated response wrapper
│
├── security/
│   ├── AuthFilter.java                      # JWT validation + rate limiting (per-IP, time-windowed)
│   ├── JwtUtil.java                         # Token generation and parsing
│   ├── AuthContext.java                     # ThreadLocal userId store (cleared after each request)
│   └── SecurityConfig.java                 # Spring Security configuration
│
├── exception/
│   ├── GlobalExceptionHandler.java         # @ControllerAdvice — maps all exceptions to HTTP responses
│   ├── AccessDeniedException.java          # 403
│   ├── UnauthorizedException.java          # 401
│   └── ResourceNotFoundException.java      # 404
│
└── FinanceApplication.java                  # Entry point + @EnableMongoAuditing
```

---

## Data Model

### Collections

```
┌──────────────────────────────────────────┐
│                 users                    │
├──────────────────────────────────────────┤
│ _id         String (ObjectId)            │
│ name        String        [required]     │
│ email       String        [unique]       │
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
│ date        LocalDateTime                │
│ notes       String        [optional]     │
│ userId      String        → users._id    │
│ deleted     Boolean       [soft delete]  │
│ createdAt   LocalDateTime [auto-audited] │
│ updatedAt   LocalDateTime [auto-audited] │
└──────────────────────────────────────────┘
```

**Relationship:** One user → many records (via `userId` reference)

**Modeling notes:**
- `deleted` is always filtered from queries and hidden from responses via `@JsonIgnore`
- `createdAt` / `updatedAt` are populated automatically by `@EnableMongoAuditing` — no manual code
- `email` has a unique index enforced at the MongoDB level via `@Indexed(unique = true)`

---

## How a Request Flows Through the System

```
HTTP Request
     │
     ▼
AuthFilter
     ├── Check rate limit (100 req/IP/minute) → 429 if exceeded
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
             ├── Fetch actual User from database
             ├── Check user.isActive() → 401 if inactive
             └── Return live User entity
     │
     ▼
Business Logic + assertAdmin() / assertNotViewer()
     └── Second role check with live user state
     │
     ▼
Repository → MongoDB
     │
     ▼
Response returned
     │
     ▼
AuthContext.clear() ← always runs in finally block, prevents ThreadLocal leakage
```

---

## Core Requirements — How Each Was Implemented

### 1. User and Role Management

The role model is three-tiered: `VIEWER`, `ANALYST`, `ADMIN`. Roles are stored as strings in MongoDB and enforced at two independent layers on every request.

- **Create user**: `POST /api/users` — open for first user (bootstrap), ADMIN-only after
- **Get current user**: `GET /api/users/me` — returns the authenticated user's profile using JWT (Small idea on my own, not mentioned in assessment but applicable in a real finance dashboard context in which allowing users to view their own profile securely)
- **Update user**: `PATCH /api/users/{id}` — ADMIN can change role or toggle `active` status. Partial update — only non-null fields in the request body are applied
- **User state**: Deactivated users (`active: false`) are rejected in `resolveCaller()` before any business logic runs, even if they hold a valid token
- **Password security**: BCrypt-hashed on creation. Plain text never stored. `UserResponse` DTO always excludes the password field

### 2. Financial Records Management

Each record stores: `amount` (validated positive Double), `type` (INCOME/EXPENSE), `category`, `date`, `notes` (optional), and `userId` (stamped automatically from the caller's JWT context on create).

- **Create**: `POST /api/records` — ADMIN only
- **Read all**: `GET /api/records` — ANALYST + ADMIN
- **Update**: `PUT /api/records/{id}` — ADMIN only, full field update
- **Delete**: `DELETE /api/records/{id}` — ADMIN only, soft delete only
- **Filter**: `GET /api/records/filter` — five optional combinable params, single dynamic query

### 3. Dashboard Summary APIs

All dashboard metrics are computed inside MongoDB via aggregation pipelines — not loaded into Java and processed in memory.

| Field | How it's computed |
|---|---|
| `totalIncome` | Aggregation: match INCOME records, sum amount |
| `totalExpense` | Aggregation: match EXPENSE records, sum amount |
| `netBalance` | totalIncome − totalExpense (computed in Java after) |
| `categoryTotals` | Aggregation: group by category, sum amount |
| `monthlyTrends` | Aggregation: group by year+month, net signed sum (INCOME positive, EXPENSE negative), IST timezone |
| `recentActivity` | Top 5 most recent non-deleted records |

### 4. Access Control Logic

Enforced at two layers:

**Layer 1 — `@PreAuthorize` at controller**: Checks the role from the JWT in the Spring SecurityContext. Fast, framework-level gate.

**Layer 2 — `resolveCaller()` + `assertAdmin()` / `assertNotViewer()` in services**: Fetches the actual user from the database and re-checks role + active status. Catches deactivated users that the annotation layer cannot catch.

### 5. Validation and Error Handling

- Bean validation on all request bodies: `@NotNull`, `@NotBlank`, `@Positive`, `@Email`
- `GlobalExceptionHandler` maps every exception type to the correct HTTP status with a consistent `{ "error": "message" }` response body:

| Exception | HTTP Status |
|---|---|
| `MethodArgumentNotValidException` | 400 — with field-level error map |
| `IllegalArgumentException` | 400 (e.g. from > to in date range) |
| `ResourceNotFoundException` | 404 |
| `UnauthorizedException` | 401 |
| `AccessDeniedException` (custom) | 403 |
| Spring's `AccessDeniedException` | 403 |
| `AuthenticationCredentialsNotFoundException` | 401 |
| Any other `Exception` | 500 |

### 6. Data Persistence

Two repository patterns used together via interface composition:

- **MongoRepository** — for standard CRUD and derived query methods (`findByDeletedFalse`, `findTop5ByDeletedFalseOrderByDateDesc`, etc.)
- **MongoTemplate + Aggregation API** (`FinancialRecordCustomRepositoryImpl`) — for dashboard aggregation pipelines and the dynamic filter query

`FinancialRecordRepository` extends both, so callers get a single interface for all data access needs.

---

## Optional Enhancements

### JWT Authentication (Access + Refresh Tokens)

Login returns both an access token (15 min expiry) and a refresh token (7 days). The refresh endpoint (`POST /api/auth/refresh`) validates the refresh token type claim, fetches the current user, and issues a new access token — without requiring re-login.

### Pagination

`GET /api/records/paginated?page=0&size=10` returns a `PageResponse<FinancialRecord>` wrapper with `data`, `page`, `size`, and `total`. Records are sorted by date descending. Invalid pagination params (negative page, zero size) return 400.

### Search

`GET /api/records/filter?search=salary` runs a case-insensitive regex search across both `category` and `notes` fields. The `search` param is additive — it can be combined with type, category, and date range filters in the same query via the Criteria builder.

### Soft Delete

Records are never physically removed. All queries and aggregations explicitly filter `deleted: false`. The `deleted` field is invisible in every API response.

### Rate Limiting

Implemented in `AuthFilter` using a `ConcurrentHashMap<IP, RequestInfo>`. Each IP gets 100 requests per minute. The window resets automatically after 60 seconds. Exceeded requests get a 429 with a JSON error body. (See tradeoffs for limitations.)

### Unit Tests

Service-layer tests using JUnit 5 + Mockito + AssertJ:

- `FinancialRecordServiceTest` — covers: create/read/delete blocked by role, soft delete verified (never calls `deleteById`), filter combinations, pagination, keyword search
- `UserServiceTest` — covers: `resolveCaller` with missing token / inactive user / active user, `getAllUsers` by role, `updateUser` (role change, deactivation), `createUser` (duplicate email, new email)

Every test uses `@AfterEach` to call `AuthContext.clear()` to prevent ThreadLocal leakage between tests.

---

## Access Control Matrix

| Action | VIEWER | ANALYST | ADMIN |
|---|---|---|---|
| Login / Refresh token | ALLOWED | ALLOWED | ALLOWED |
| View dashboard summary | ALLOWED | ALLOWED | ALLOWED |
| View / filter records | NOT ALLOWED | ALLOWED | ALLOWED |
| Paginated records | NOT ALLOWED | ALLOWED | ALLOWED |
| Create records | NOT ALLOWED | NOT ALLOWED | ALLOWED |
| Update records | NOT ALLOWED | NOT ALLOWED | ALLOWED |
| Delete records | NOT ALLOWED | NOT ALLOWED | ALLOWED |
| Manage users | NOT ALLOWED | NOT ALLOWED | ALLOWED |

---

## API Documentation

All protected endpoints require:
```
Authorization: Bearer <access_token>
```

---

### Auth

#### `POST /api/auth/login`

**Request:**
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

**Errors:** `401` invalid credentials, `401` inactive account

---

#### `POST /api/auth/refresh`

**Request:**
```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Response 200:** New `accessToken`, same `refreshToken`

**Errors:** `401` invalid or non-refresh token

---

### Users

#### `POST /api/users`
No auth for first user. ADMIN token required after.

**Request:**
```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "pass123",
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

**Errors:** `400` validation errors, `400` duplicate email, `403` non-admin after bootstrap

---

#### `GET /api/users` — ADMIN only
Returns list of all users (no password field).

#### `GET /api/users/{id}` — ADMIN only
**Errors:** `404` user not found

#### `PATCH /api/users/{id}` — ADMIN only
Partial update. Only provided fields are changed.

**Request:**
```json
{
  "role": "VIEWER",
  "active": false
}
```

---

### Financial Records

#### `POST /api/records` — ADMIN only

**Request:**
```json
{
  "amount": 75000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-01-15T00:00:00",
  "notes": "January salary"
}
```

**Response 201:** Full record object including `userId`, `createdAt`, `updatedAt`

**Errors:** `400` validation, `403` non-admin

---

#### `GET /api/records` — ANALYST + ADMIN
All non-deleted records.

---

#### `GET /api/records/filter` — ANALYST + ADMIN

All params optional, all combinable:

| Param | Type | Description |
|---|---|---|
| `type` | `INCOME` or `EXPENSE` | Filter by type |
| `category` | String | Exact category match |
| `from` | `YYYY-MM-DD` | Start of date range |
| `to` | `YYYY-MM-DD` | End of date range |
| `search` | String | Regex search across category + notes |

**Examples:**
```
GET /api/records/filter?type=INCOME
GET /api/records/filter?category=Salary&from=2025-01-01&to=2025-01-31
GET /api/records/filter?search=electricity&type=EXPENSE
GET /api/records/filter?from=2025-01-01&to=2025-03-31
```

**Errors:** `400` if `from` is after `to`

---

#### `GET /api/records/paginated` — ANALYST + ADMIN

| Param | Default | Description |
|---|---|---|
| `page` | `0` | Page index (0-based) |
| `size` | `10` | Records per page |

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

#### `PUT /api/records/{id}` — ADMIN only
Full field update. Returns updated record.
**Errors:** `404` not found, `403` non-admin

---

#### `DELETE /api/records/{id}` — ADMIN only
Soft delete. **Response 204** — no content.
Record remains in database with `deleted = true`.

---

### Dashboard

#### `GET /api/dashboard/summary` — All roles

**Response 200:**
```json
{
  "totalIncome": 150000.0,
  "totalExpense": 45000.0,
  "netBalance": 105000.0,
  "categoryTotals": {
    "Salary": 150000.0,
    "Utilities": 12000.0,
    "Rent": 33000.0
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

`monthlyTrends` values are net per month — positive means net income, negative means net expense.

---

## Setup and Running Locally

### Prerequisites
- Java 17+
- Maven 3.8+
- MongoDB on `localhost:27017` (or a remote/Atlas URI)

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/pranavsaai/finance-backend.git
cd finance-backend

# 2. Set environment variables
export MONGO_URI_FINANCE=mongodb://localhost:27017/finance_db
export JWT_SECRET_FINANCE=your-secret-key-minimum-32-characters-long

# 3. Run
mvn spring-boot:run
```

Server starts at `http://localhost:8080`.

### Running Tests
```bash
mvn test
```

The Spring context test (`FinanceApplicationTests`) requires a MongoDB instance at `mongodb://localhost:27017/finance_test`.

---

## Environment Variables

Create a `.env` file based on `.env.example` and configure the following:

| Variable | Description |
|---|---|
| `MONGO_URI_FINANCE` | MongoDB connection string |
| `JWT_SECRET_FINANCE` | JWT signing secret (min 32 characters) |

Access tokens expire in **15 minutes**. Refresh tokens expire in **7 days**.

---

## Known Tradeoffs and Future Improvements

### Rate Limiting — In-Memory Only
The current rate limiter uses a `ConcurrentHashMap` inside `AuthFilter`. It uses a fixed time window (100 req/IP/minute) that resets automatically. It works correctly for single-instance deployments but resets on restart and won't work across multiple instances.

**Production approach**: Replace with a distributed rate limiter backed by Redis using a sliding window or token bucket algorithm (e.g. Bucket4j with Redis).

### Bootstrap Endpoint
`POST /api/users` stays `permitAll()` to allow the first admin to be created. After that, the application-level check takes over. This is intentional for assessment scope.

**Production approach**: Disable the public route after bootstrap automatically, or use an invite/onboarding flow.

### Search Does Not Combine With All Filters
When `search` is provided, it is combined with type, category, and date range through the Criteria builder. However, search uses a regex match which is not index-backed by default in MongoDB.

**Production approach**: Use MongoDB Atlas Search or a text index for scalable full-text search.

### Pagination Without a Size Cap
Page size is validated (must be > 0) but not capped. A client could request `size=100000`.

**Production approach**: Enforce a maximum page size (e.g. 100 records) to prevent oversized queries.

### Soft Delete Without Archival
Deleted records stay in the primary `records` collection indefinitely. At scale this grows the collection unnecessarily.

**Production approach**: Move deleted records to an archive collection, or apply a TTL index to clean up old soft-deleted records after a retention period.

### Dual-Layer Security — Slight Duplication
`@PreAuthorize` and `resolveCaller()` both check roles. This is intentional for defense-in-depth, but it means role logic is defined in two places.

**Production approach**: Consolidate into a centralized policy layer or AOP-based authorization advice, keeping service methods clean while still catching inactive user state.

### Logging
Current logging is minimal (console only, a few debug prints).

**Production approach**: Structured JSON logging with request IDs, integrated with an observability stack (ELK, Datadog, etc.).

### Limited Controller-Level Testing

Unit tests are primarily focused on the service layer, where core business logic and validations reside. This ensures that the most critical parts of the application are thoroughly tested.

Controller-level tests (e.g., using MockMvc) were not included to keep the test setup simple and avoid additional complexity around test data seeding (e.g., creating users for JWT-based authentication).

**Tradeoff**:
This approach reduces test coverage at the API layer, particularly for security and request/response validation.

**Future improvement**:
Controller and integration tests can be added using MockMvc or Testcontainers with proper test data setup to validate end-to-end request flows, including authentication and role-based access control.