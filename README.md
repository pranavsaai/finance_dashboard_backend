# Finance Dashboard Backend

A production-structured REST API backend for a **role-based finance dashboard system** built with **Spring Boot**, **MongoDB**, and **JWT authentication**. Supports full financial record management, aggregated dashboard analytics, and strict access control enforced at both the security filter and service layers.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Data Model / ER Diagram](#data-model--er-diagram)
- [Process Flow — End to End](#process-flow--end-to-end)
- [Core Requirements — How Each Was Implemented](#core-requirements--how-each-was-implemented)
- [Optional Enhancements — How Each Was Implemented](#optional-enhancements--how-each-was-implemented)
- [Setup and Running Locally](#setup-and-running-locally)
- [Environment Variables](#environment-variables)
- [API Documentation](#api-documentation)
- [Known Tradeoffs and Future Improvements](#known-tradeoffs-and-future-improvements)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Database | MongoDB (via Spring Data MongoDB) |
| Security | Spring Security + JWT (JJWT library) |
| Password Hashing | BCrypt (Spring Security Crypto) |
| Validation | Jakarta Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`, etc.) |
| Boilerplate Reduction | Lombok (`@Data`, `@RequiredArgsConstructor`, etc.) |
| Testing | JUnit 5 + Mockito + AssertJ |
| Build Tool | Maven |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP Request                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AuthFilter (OncePerRequestFilter)            │
│   - Rate limiting (100 req/IP)                                  │
│   - JWT validation → extracts userId + role                     │
│   - Populates Spring SecurityContext                            │
│   - Sets AuthContext (ThreadLocal) with userId                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│               Spring Security (@PreAuthorize)                   │
│   - Role-level gate at controller method                        │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Controllers                              │
│   AuthController / UserController /                             │
│   FinancialRecordController / DashboardController               │
│   - Input validation (@Valid)                                   │
│   - HTTP response mapping                                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Services                                │
│   UserService / FinancialRecordService / DashboardService       │
│   - Business logic                                              │
│   - Second layer of role enforcement (resolveCaller + assert*)  │
│   - Entity state management                                     │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Repositories                              │
│   UserRepository / FinancialRecordRepository                    │
│   FinancialRecordCustomRepositoryImpl (MongoTemplate)           │
│   - Spring Data derived queries                                 │
│   - MongoDB aggregation pipelines                               │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
                      [ MongoDB ]
```

**Dual-layer access control** is a deliberate design choice — Spring Security `@PreAuthorize` enforces roles at the HTTP boundary, and service-layer `resolveCaller()` + `assertAdmin()` / `assertNotViewer()` enforce them again with domain-level context (e.g. checking if the user is inactive). This means even if the security filter were misconfigured, the service layer would still reject unauthorized operations.

---

## Project Structure

```
com.zorvyn.finance/
│
├── controller/
│   ├── AuthController.java             # POST /api/auth/login
│   ├── UserController.java             # User CRUD + role management
│   ├── FinancialRecordController.java  # Record CRUD + filter + pagination
│   └── DashboardController.java        # Dashboard summary endpoint
│
├── service/
│   ├── UserService.java                # User business logic + resolveCaller
│   ├── FinancialRecordService.java     # Record business logic + access checks
│   └── DashboardService.java           # Aggregation orchestration
│
├── repository/
│   ├── UserRepository.java                          # MongoRepository for users
│   ├── FinancialRecordRepository.java               # MongoRepository + derived queries
│   ├── FinancialRecordCustomRepository.java         # Interface for aggregations
│   └── FinancialRecordCustomRepositoryImpl.java     # MongoTemplate aggregation pipelines
│
├── entity/
│   ├── User.java                       # User document model
│   ├── FinancialRecord.java            # Financial record document model
│   ├── Role.java                       # Enum: VIEWER, ANALYST, ADMIN
│   └── RecordType.java                 # Enum: INCOME, EXPENSE
│
├── dto/
│   ├── LoginRequest.java               # Email + password payload
│   ├── UserResponse.java               # Safe user response (no password)
│   ├── UserUpdateRequest.java          # Partial update: role and/or active status
│   ├── DashboardSummary.java           # Aggregated dashboard response
│   └── PageResponse.java              # Generic paginated response wrapper
│
├── security/
│   ├── AuthFilter.java                 # JWT filter + rate limiting
│   ├── JwtUtil.java                    # Token generation + parsing
│   ├── AuthContext.java                # ThreadLocal userId store
│   └── SecurityConfig.java            # Spring Security configuration
│
├── exception/
│   ├── GlobalExceptionHandler.java     # @ControllerAdvice error handler
│   ├── AccessDeniedException.java      # 403 - custom
│   ├── UnauthorizedException.java      # 401 - custom
│   └── ResourceNotFoundException.java # 404 - custom
│
└── FinanceApplication.java             # Entry point + @EnableMongoAuditing
```

---

## Data Model / ER Diagram

### Collections

```
┌──────────────────────────────────────┐
│               users                  │
├──────────────────────────────────────┤
│ _id           : String (ObjectId)    │
│ name          : String  [required]   │
│ email         : String  [unique]     │
│ password      : String  [hashed]     │
│ role          : VIEWER|ANALYST|ADMIN │
│ active        : Boolean (default: true) │
│ createdAt     : LocalDateTime        │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│              records                 │
├──────────────────────────────────────┤
│ _id           : String (ObjectId)    │
│ amount        : Double  [required, > 0] │
│ type          : INCOME | EXPENSE     │
│ category      : String  [required]   │
│ date          : LocalDateTime        │
│ notes         : String  [optional]   │
│ userId        : String  → users._id  │
│ deleted       : Boolean (soft delete) │
│ createdAt     : LocalDateTime (audit) │
│ updatedAt     : LocalDateTime (audit) │
└──────────────────────────────────────┘
```

### Relationship

```
users (1) ────────────────────── (many) records
          userId in records references _id in users
```

### Notes on Data Modeling Decisions

- **MongoDB** was chosen for its flexible document model, which pairs naturally with financial entries that may evolve in structure (e.g. adding new metadata fields).
- **Soft delete** (`deleted: Boolean`) is used on records rather than physical deletion. This preserves audit history and allows future recovery. The `deleted` field is annotated with `@JsonIgnore` so it is never exposed in API responses.
- **`@CreatedDate` / `@LastModifiedDate`** are powered by Spring Data MongoDB Auditing (`@EnableMongoAuditing`) — these are populated automatically without any manual code.
- **Unique index** on `users.email` is declared via `@Indexed(unique = true)` and enforced at the MongoDB level with `spring.data.mongodb.auto-index-creation=true`.

---

## Process Flow — End to End

### 1. System Bootstrap (First User Creation)

When the database is empty, the `POST /api/users` endpoint is publicly accessible. The first request creates the initial Admin user. All subsequent user creation requests require an authenticated ADMIN token.

```
POST /api/users  (no auth required if DB is empty)
        │
        ▼
UserController.create()
        │
        ├── userService.isFirstUser() → true? → proceed directly
        │
        └── else → userService.assertAdmin() → checks JWT + role
                │
                ▼
        userService.createUser()
                │
                ├── Check duplicate email (existsByEmail)
                ├── BCrypt hash the password
                └── Save to MongoDB → return UserResponse (no password)
```

### 2. Authentication Flow

```
POST /api/auth/login  { email, password }
        │
        ▼
AuthController.login()
        │
        ├── Lookup user by email (or throw 404)
        ├── BCrypt.matches(raw, hashed) (or throw 401)
        └── JwtUtil.generateToken(userId, role)
                │
                └── Returns: JWT string (Bearer token)
```

JWT payload contains:
- `sub`: userId
- `role`: VIEWER / ANALYST / ADMIN
- `iat`: issued at
- `exp`: expiry (24 hours)

### 3. Authenticated Request Flow

```
Any protected request with Authorization: Bearer <token>
        │
        ▼
AuthFilter.doFilterInternal()
        │
        ├── Rate limit check: > 100 requests/IP → 429
        │
        ├── Extract token from Authorization header
        ├── JwtUtil.extractUserId(token) → userId
        ├── JwtUtil.extractRole(token) → role
        ├── Set Spring SecurityContext (ROLE_ADMIN / ROLE_ANALYST / ROLE_VIEWER)
        ├── Set AuthContext.set(userId) → ThreadLocal
        │
        ▼
@PreAuthorize("hasRole('ADMIN')") — controller-level gate
        │
        ▼
Service method: userService.resolveCaller()
        │
        ├── AuthContext.get() → userId
        ├── userRepository.findById(userId)
        ├── Check caller.isActive() → throw 401 if inactive
        └── Return User entity for further role logic
        │
        ▼
Business logic executes → response returned
        │
        ▼
AuthContext.clear() → ThreadLocal cleared in finally block
```

### 4. Financial Record Operations

**Create (ADMIN only):**
```
POST /api/records  { amount, type, category, date, notes }
        │
        ▼
assertAdmin(caller) → save record with userId = caller.getId()
```

**Filter (ANALYST + ADMIN):**
```
GET /api/records/filter?type=INCOME&category=Salary&from=2025-01-01&to=2025-01-31
        │
        ▼
filterRecords(type, category, from, to, search)
        │
        ├── If 'search' keyword provided → MongoDB regex search (category + notes)
        ├── Else → select from 8 derived query combinations:
        │   type + category + dateRange → findByTypeAndCategoryAndDateBetween...
        │   type + dateRange            → findByTypeAndDateBetween...
        │   category + dateRange        → findByCategoryAndDateBetween...
        │   type + category             → findByTypeAndCategory...
        │   dateRange only              → findByDateBetween...
        │   type only                   → findByType...
        │   category only               → findByCategory...
        │   none                        → findAll (non-deleted)
        └── All queries filter deleted=false
```

**Soft Delete (ADMIN only):**
```
DELETE /api/records/{id}
        │
        ▼
record.setDeleted(true) → recordRepository.save(record)
        (physical deletion never occurs)
```

### 5. Dashboard Summary Flow

```
GET /api/dashboard/summary  (all roles)
        │
        ▼
DashboardService.getSummary()
        │
        ├── resolveCaller() → validate user is active
        │
        ├── getTotalIncome()   → MongoDB aggregation: match INCOME + sum amount
        ├── getTotalExpense()  → MongoDB aggregation: match EXPENSE + sum amount
        ├── netBalance         → totalIncome - totalExpense (computed in Java)
        │
        ├── getCategoryTotals() → MongoDB aggregation: group by category + sum amount
        │
        ├── getMonthlyTrends()  → MongoDB aggregation:
        │   ├── Project: extract year + month from date (IST timezone)
        │   ├── Group by year+month: sum(INCOME as positive, EXPENSE as negative)
        │   └── Sort by year+month ascending → LinkedHashMap (insertion-ordered)
        │
        └── recentActivity → findTop5ByDeletedFalseOrderByDateDesc()
                           → mapped to lightweight Map<String, Object> entries
```

---

## Core Requirements — How Each Was Implemented

### 1. User and Role Management

- **Creating users**: `POST /api/users` — open for first user (bootstrap), ADMIN-only thereafter.
- **Role model**: Three roles defined as a Java enum — `VIEWER`, `ANALYST`, `ADMIN` — stored as strings in MongoDB.
- **Managing status**: `PATCH /api/users/{id}` allows ADMIN to update a user's role or set `active: false` (deactivate). Partial update — only non-null fields in the request body are applied.
- **Restricting by role**: Done at two layers — `@PreAuthorize` at the controller and `assertAdmin()` / `assertNotViewer()` inside services. Inactive users are rejected during `resolveCaller()` before any logic runs.
- **Password security**: Passwords are BCrypt-hashed on creation. Plain text is never stored. `UserResponse` DTO deliberately excludes the password field from all API responses.

### 2. Financial Records Management

- **Fields**: `amount` (validated positive Double), `type` (INCOME/EXPENSE enum), `category` (required String), `date` (LocalDateTime), `notes` (optional), plus audit fields `createdAt`/`updatedAt` via Spring Auditing.
- **Create**: `POST /api/records` — ADMIN only. Sets `userId` automatically from JWT context.
- **Read**: `GET /api/records` — ANALYST + ADMIN. Returns all non-deleted records.
- **Update**: `PUT /api/records/{id}` — ADMIN only. Full update of all mutable fields.
- **Delete**: `DELETE /api/records/{id}` — ADMIN only. Soft delete (sets `deleted=true`), never physically removed.
- **Filter**: `GET /api/records/filter` — supports type, category, date range, and keyword search — all optional and combinable. Eight distinct repository query paths handle every valid combination.

### 3. Dashboard Summary APIs

All fields in `DashboardSummary` are computed via MongoDB aggregation pipelines (not in-memory Java streams), making the dashboard efficient even at large data volumes:

| Field | Method |
|---|---|
| `totalIncome` | Aggregation: match INCOME, sum amount |
| `totalExpense` | Aggregation: match EXPENSE, sum amount |
| `netBalance` | Computed: totalIncome − totalExpense |
| `categoryTotals` | Aggregation: group by category, sum amount |
| `monthlyTrends` | Aggregation: group by year+month, net signed sum |
| `recentActivity` | Derived query: top 5 most recent non-deleted records |

### 4. Access Control Logic

| Action | VIEWER | ANALYST | ADMIN |
|---|---|---|---|
| Login | ALLOWED | ALLOWED | ALLOWED |
| View dashboard summary | ALLOWED | ALLOWED | ALLOWED |
| View/filter records | NOT ALLOWED | ALLOWED | ALLOWED |
| Paginated records | NOT ALLOWED | ALLOWED | ALLOWED |
| Create records | NOT ALLOWED | NOT ALLOWED | ALLOWED |
| Update records | NOT ALLOWED | NOT ALLOWED | ALLOWED |
| Delete records | NOT ALLOWED | NOT ALLOWED | ALLOWED |
| Manage users | NOT ALLOWED | NOT ALLOWED | ALLOWED |

Implementation method: `@PreAuthorize` annotations at the controller layer + manual `assertAdmin()` / `assertNotViewer()` in service methods.

### 5. Validation and Error Handling

- **Bean Validation** on entities: `@NotNull`, `@NotBlank`, `@Positive`, `@Email`. Triggered via `@Valid` on controller method parameters.
- **GlobalExceptionHandler** (`@ControllerAdvice`) handles:
  - `MethodArgumentNotValidException` → 400 with field-level error map
  - `IllegalArgumentException` → 400 (e.g. invalid date range: from > to)
  - `ResourceNotFoundException` → 404
  - `UnauthorizedException` → 401 (missing/inactive user)
  - `AccessDeniedException` (custom) → 403
  - `org.springframework.security.access.AccessDeniedException` → 403
  - `AuthenticationCredentialsNotFoundException` → 401
  - `Exception` (fallback) → 500
- All error responses return a consistent JSON shape: `{ "error": "message" }`

### 6. Data Persistence

MongoDB is used as the primary database. Two repository patterns are used:

- **Spring Data MongoRepository**: For standard CRUD and derived queries (e.g. `findByTypeAndCategoryAndDeletedFalse`).
- **MongoTemplate with Aggregation API** (`FinancialRecordCustomRepositoryImpl`): For dashboard aggregations — total income/expense, category totals, and monthly net trends.

The two patterns are combined via interface composition: `FinancialRecordRepository` extends both `MongoRepository` and `FinancialRecordCustomRepository`.

---

## Optional Enhancements — How Each Was Implemented

### A1.) JWT Authentication

Full token-based authentication using the JJWT library. On login, a signed JWT is returned containing `userId` and `role` as claims. The `AuthFilter` validates and parses this token on every request, populating both the Spring `SecurityContext` and the `AuthContext` ThreadLocal.

### 2.) Pagination

`GET /api/records/paginated?page=0&size=10` returns a `PageResponse<FinancialRecord>` with `data`, `page`, `size`, and `total` fields. Uses Spring Data's `Pageable` interface with descending sort by date. Input validation rejects negative page or zero size with a 400 error.

### 3.) Search

`GET /api/records/filter?search=salary` performs a case-insensitive MongoDB regex search across both the `category` and `notes` fields. Implemented as a `@Query` annotation on the repository interface:
```
{ deleted: false, $or: [ { category: { $regex: ?0, $options: 'i' } }, { notes: { $regex: ?0, $options: 'i' } } ] }
```
When `search` is provided, it takes priority over all other filter parameters.

### 3.) Soft Delete

Records are never physically removed from the database. `DELETE /api/records/{id}` sets `deleted = true` and saves the record. All queries and aggregations explicitly filter `deleted: false`. The `deleted` field is annotated with `@JsonIgnore` so it is invisible in every API response.

### 4.) Rate Limiting

Implemented directly in `AuthFilter` using a `ConcurrentHashMap<String, Integer>` keyed by client IP address. Once a single IP exceeds 100 requests, subsequent requests receive HTTP 429 with the message `Too many requests`.

**Current behavior and known limitation**: The counter resets on application restart and is stored in memory only — it does not persist across instances. This is sufficient for single-instance local development. A production-grade implementation would use Redis with a sliding window or token bucket algorithm. This is documented as a planned improvement.

### 5.) Unit Tests

Service-layer unit tests are written using JUnit 5 + Mockito + AssertJ covering:

- `FinancialRecordServiceTest`: create/read/update/delete access denied by role, soft delete verified, filter combinations, pagination, keyword search
- `UserServiceTest`: `resolveCaller` (missing header, inactive user, active user), getAllUsers access by role, updateUser (role change, deactivation), createUser (duplicate email, new email)
- `FinanceApplicationTests`: Spring context load verification

Every test uses `@BeforeEach` to set up user fixtures and `@AfterEach` to call `AuthContext.clear()` to prevent thread-local leakage between tests.

---

## Setup and Running Locally

### Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB running locally on port `27017` (or a connection string to a remote/Atlas instance)

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/your-username/finance-backend.git
cd finance-backend

# 2. Set environment variables (see section below)
export MONGO_URI_FINANCE=mongodb://localhost:27017/finance_db
export JWT_SECRET_FINANCE=your-very-long-secret-key-at-least-32-chars

# 3. Build and run
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

### Running Tests

```bash
mvn test
```

The Spring context integration test (`FinanceApplicationTests`) requires a local MongoDB at `mongodb://localhost:27017/finance_test`.

---

## Environment Variables

| Variable | Description | Example |
|---|---|---|
| `MONGO_URI_FINANCE` | MongoDB connection URI | `mongodb://localhost:27017/finance_db` |
| `JWT_SECRET_FINANCE` | Secret key for JWT signing (min 32 chars) | `my-super-secret-finance-key-2025` |

JWT tokens expire after **24 hours** (`jwt.expiration=86400000` ms).

---

## API Documentation

All protected endpoints require the header:
```
Authorization: Bearer <token>
```

---

### Auth

#### POST /api/auth/login
Login and receive a JWT token.

**Request:**
```json
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "secret123"
}
```

**Response (200):**
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NjFhM...
```

**Error responses:**
- `404` — User not found
- `401` — Invalid credentials

---

### Users

#### POST /api/users
Create a user. No auth required for the very first user (bootstrap). ADMIN token required for all subsequent calls.

**Request:**
```json
POST /api/users
Content-Type: application/json

{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "pass123",
  "role": "ADMIN"
}
```

**Response (201):**
```json
{
  "id": "661a3f...",
  "name": "Alice",
  "email": "alice@example.com",
  "role": "ADMIN",
  "active": true,
  "createdAt": "2025-01-15T10:00:00"
}
```

**Validation errors (400):**
```json
{
  "email": "Provide a valid email address",
  "name": "Name is required"
}
```

---

#### GET /api/users
Get all users. Requires ADMIN.

**Response (200):**
```json
[
  {
    "id": "661a3f...",
    "name": "Alice",
    "email": "alice@example.com",
    "role": "ADMIN",
    "active": true,
    "createdAt": "2025-01-15T10:00:00"
  }
]
```

---

#### GET /api/users/{id}
Get a user by ID. Requires ADMIN.

**Response (200):** Same as single user object above.

**Error (404):**
```json
{ "error": "User not found with id: abc123" }
```

---

#### PATCH /api/users/{id}
Update a user's role or active status. Requires ADMIN. Fields are optional — only provided fields are updated.

**Request:**
```json
PATCH /api/users/661a3f...
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "role": "ANALYST",
  "active": false
}
```

**Response (200):** Updated user object.

---

### Financial Records

#### POST /api/records
Create a financial record. Requires ADMIN.

**Request:**
```json
POST /api/records
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "amount": 75000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-01-15T00:00:00",
  "notes": "January salary"
}
```

**Response (201):**
```json
{
  "id": "rec-abc123",
  "amount": 75000.0,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-01-15T00:00:00",
  "notes": "January salary",
  "userId": "661a3f...",
  "createdAt": "2025-01-15T10:05:00",
  "updatedAt": "2025-01-15T10:05:00"
}
```

**Error (403):**
```json
{ "error": "Only ADMIN can perform this action" }
```

---

#### GET /api/records
Get all non-deleted records. Requires ANALYST or ADMIN.

**Response (200):**
```json
[
  {
    "id": "rec-abc123",
    "amount": 75000.0,
    "type": "INCOME",
    "category": "Salary",
    "date": "2025-01-15T00:00:00",
    "notes": "January salary",
    "userId": "661a3f...",
    "createdAt": "2025-01-15T10:05:00",
    "updatedAt": "2025-01-15T10:05:00"
  }
]
```

---

#### GET /api/records/filter
Filter records. All params optional and combinable. Requires ANALYST or ADMIN.

| Param | Type | Description |
|---|---|---|
| `type` | `INCOME` or `EXPENSE` | Filter by record type |
| `category` | String | Filter by category |
| `from` | `YYYY-MM-DD` | Start of date range |
| `to` | `YYYY-MM-DD` | End of date range |
| `search` | String | Keyword search across category and notes |

**Examples:**

```
GET /api/records/filter?type=INCOME
GET /api/records/filter?category=Salary&from=2025-01-01&to=2025-01-31
GET /api/records/filter?type=EXPENSE&category=Utilities
GET /api/records/filter?search=electricity
GET /api/records/filter?from=2025-01-01&to=2025-03-31
```

**Error (400) — invalid date range:**
```json
{ "error": "From date cannot be after To date" }
```

---

#### GET /api/records/paginated
Get paginated records, sorted by date descending. Requires ANALYST or ADMIN.

| Param | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `10` | Records per page |

**Example:**
```
GET /api/records/paginated?page=0&size=5
```

**Response (200):**
```json
{
  "data": [ ... ],
  "page": 0,
  "size": 5,
  "total": 42
}
```

---

#### PUT /api/records/{id}
Full update of a record. Requires ADMIN.

**Request:**
```json
PUT /api/records/rec-abc123
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "amount": 80000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-02-15T00:00:00",
  "notes": "Updated salary"
}
```

**Response (200):** Updated record object.

**Error (404):**
```json
{ "error": "Record not found with id: rec-abc123" }
```

---

#### DELETE /api/records/{id}
Soft delete a record. Requires ADMIN. Record is marked `deleted=true` and excluded from all future queries. The record is never physically removed from the database.

**Response (204):** No content.

---

### Dashboard

#### GET /api/dashboard/summary
Returns aggregated financial summary. Accessible by all roles (VIEWER, ANALYST, ADMIN).

**Response (200):**
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
    "2025-02": 50000.0
  }
}
```

`monthlyTrends` values are net (income positive, expenses negative and subtracted). A positive value means net income for that month, negative means net expense.

---

## Known Tradeoffs and Future Improvements

### Rate Limiting (In-Memory)
The current implementation uses a ConcurrentHashMap<IP, count> in AuthFilter. This resets on restart and does not work in a multi-instance deployment.

**Planned improvement**: Replace with Redis-backed rate limiting using a sliding window algorithm. A library like Bucket4j with a Redis backend would support distributed deployments and configurable time windows (for example: 100 requests per minute per IP, not unbounded).

### Bootstrap User Creation (Public Endpoint)
The /api/users endpoint is intentionally left permitAll() to allow initial system bootstrapping (first admin creation). After the first user is created, access is restricted via application-level checks.

**Planned improvement**: Automatically disable public access after the first admin is created or introduce invite-based user onboarding to enforce controlled role assignment.

### Service-Level + Annotation-Based Security (Dual Enforcement)
Role-based access is enforced both via @PreAuthorize annotations and service-layer checks. While this provides defense-in-depth, it introduces slight duplication.

**Planned improvement**: Consolidate security rules into a centralized policy layer or rely more on Spring Security expressions while keeping minimal service-level safeguards.

### Search and Filter Combination Logic
When a search keyword is provided to GET /api/records/filter, it overrides other filters (type, category, date range) for simplicity.

**Planned improvement**: Extend query logic to combine search with structured filters, allowing more flexible querying (e.g. keyword + date range + type).

### Date Handling Normalization
The system stores dates as LocalDateTime while accepting filter inputs as LocalDate. These are normalized internally to full-day ranges.

**Planned improvement**: Introduce a dedicated query DTO or utility layer to standardize date handling and improve readability and reusability.

### Pagination Validation and Limits
Pagination parameters (page, size) are validated but not capped.

**Planned improvement**: Enforce maximum page size limits (e.g. 100 records) to prevent excessive data retrieval and improve performance under load.

### Soft Delete Without Archival Strategy
Records are soft-deleted using a deleted flag but remain in the primary collection.

**Planned improvement**: Move deleted records to an archive collection or implement TTL/index-based cleanup for long-term data management.

### Logging and Monitoring
Current logging is minimal and primarily console-based.

**Planned improvement**: Introduce structured logging (e.g. JSON logs) and integrate with monitoring tools like ELK stack or Prometheus/Grafana for better observability in production.
