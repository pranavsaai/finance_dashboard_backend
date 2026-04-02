# Finance Dashboard Backend

A role-based finance dashboard backend built as a developer assessment. Covers financial record management, user roles, JWT-based access control, and dashboard analytics.

**Stack:** Java 17 · Spring Boot 3.2.4 · MongoDB · JWT (jjwt) · Spring Security · Lombok · JUnit 5 + Mockito · OpenAPI (Swagger)

---

## Table of Contents

- [Project Structure](#project-structure)
- [How It Works — End to End](#how-it-works--end-to-end)
- [Data Models](#data-models)
- [Role Permission Matrix](#role-permission-matrix)
- [Setup](#setup)
- [API Reference](#api-reference)
- [Unit Tests](#unit-tests)
- [Assumptions & Tradeoffs](#assumptions--tradeoffs)

---

## Project Structure

```
src/main/java/com/zorvyn/finance/
│
├── controller/       HTTP layer — routes, delegates to services
│   ├── AuthController.java
│   ├── UserController.java
│   ├── FinancialRecordController.java
│   └── DashboardController.java
│
├── service/          Business logic + role enforcement
│   ├── UserService.java
│   ├── FinancialRecordService.java
│   └── DashboardService.java
│
├── repository/       Spring Data MongoDB interfaces
│   ├── UserRepository.java
│   └── FinancialRecordRepository.java
│
├── entity/           MongoDB documents
│   ├── User.java
│   ├── FinancialRecord.java
│   ├── Role.java           (enum: VIEWER, ANALYST, ADMIN)
│   └── RecordType.java     (enum: INCOME, EXPENSE)
│
├── dto/              Request/response shapes
│   ├── LoginRequest.java
│   ├── UserUpdateRequest.java
│   ├── DashboardSummary.java
│   └── PageResponse.java
│
├── security/         Auth infrastructure
│   ├── AuthFilter.java     reads JWT from Authorization header + rate limiting
│   ├── AuthContext.java    ThreadLocal storage for current user ID
│   ├── JwtUtil.java        token generation and validation (HMAC-SHA256)
│   └── SecurityConfig.java Spring Security filter chain
│
└── exception/        Custom exceptions + global error handler
    ├── GlobalExceptionHandler.java
    ├── AccessDeniedException.java
    ├── UnauthorizedException.java
    └── ResourceNotFoundException.java
```

Each layer has one responsibility. Controllers don't touch the database. Services don't know about HTTP. Repositories contain no business logic.

---

## How It Works — End to End

### 1. Authentication flow

```
POST /api/auth/login  { email, password }
         │
         ▼
  AuthController
  Looks up user by email → BCrypt.matches(password, stored hash)
         │
         ▼
  JwtUtil.generateToken(userId)
  Signs JWT with HMAC-SHA256, subject = MongoDB user ID
         │
         ▼
  Returns JWT string → client stores and sends as: Authorization: Bearer <token>
```

---

### 2. Request lifecycle (authenticated endpoints)

```
Incoming HTTP request
        │
        ▼
  AuthFilter  (OncePerRequestFilter)
  ├─ Rate limit: >100 requests/IP → 429
  ├─ Reads Authorization: Bearer <token>
  ├─ JwtUtil.extractUserId(token) → validates signature, extracts userId
  ├─ AuthContext.set(userId)       → stores in ThreadLocal
  └─ Sets Spring SecurityContext authentication
        │
        ▼
  SecurityConfig
  /api/auth/** and POST /api/users → permitAll
  Everything else → must be authenticated
        │
        ▼
  Controller
  Reads params / request body, delegates to service
        │
        ▼
  Service
  AuthContext.get() → fetch User from DB → check role → run logic
        │
        ▼
  Repository
  Spring Data MongoDB executes query
        │
        ▼
  Response JSON  (or GlobalExceptionHandler → error JSON on any exception)
        │
        ▼
  AuthFilter finally block: AuthContext.clear()   ← prevents ThreadLocal leaks
```

---

### 3. Role enforcement (inside service methods)

Every protected service method starts with one of these before running any logic:

```java
resolveCaller()      // any authenticated active user → throws 401 if missing/inactive
assertAdmin()        // ADMIN only → throws 403 for ANALYST or VIEWER
assertNotViewer()    // ANALYST or ADMIN → throws 403 for VIEWER
```

`resolveCaller()` fetches the user from MongoDB using the ID stored by `AuthContext`. If the account is inactive it throws `UnauthorizedException` — deactivation takes effect on the very next request.

---

### 4. Dashboard aggregation flow

```
GET /api/dashboard/summary
        │
        ▼
  DashboardService.getSummary()
  ├─ resolveCaller()                           → any role allowed
  ├─ recordRepository.findByDeletedFalse()     → all non-deleted records
  ├─ Stream → filter INCOME → sum()           → totalIncome
  ├─ Stream → filter EXPENSE → sum()          → totalExpense
  ├─ totalIncome - totalExpense               → netBalance
  ├─ groupingBy(category, summingDouble)      → categoryTotals (Map)
  ├─ findTop5ByDeletedFalseOrderByDateDesc()  → recentActivity (last 5)
  └─ buildMonthlyTrends() → TreeMap<"YYYY-MM", net> → monthlyTrends (sorted)
```

`TreeMap` is used for monthly trends so months sort chronologically without extra sorting logic.

---

### 5. Filter routing (record filtering)

`GET /api/records/filter` accepts `type`, `category`, `from`, `to`, `search` — all optional.

When `search` is provided it runs a case-insensitive partial match on category and ignores all other params (quick-lookup mode). Otherwise all provided params are ANDed, routed to one of eight dedicated Spring Data query methods:

```
no params                          → findByDeletedFalse()
type only                          → findByTypeAndDeletedFalse()
category only                      → findByCategoryAndDeletedFalse()
date range only                    → findByDateBetweenAndDeletedFalse()
type + category                    → findByTypeAndCategoryAndDeletedFalse()
type + date range                  → findByTypeAndDateBetweenAndDeletedFalse()
category + date range              → findByCategoryAndDateBetweenAndDeletedFalse()
type + category + date range       → findByTypeAndCategoryAndDateBetweenAndDeletedFalse()
search keyword                     → findByCategoryContainingIgnoreCaseAndDeletedFalse()
```

Spring Data generates actual MongoDB queries from method names — no hand-written query code. Every method carries `AndDeletedFalse` so soft-deleted records are excluded automatically.

---

### 6. Soft delete

`DELETE /api/records/{id}` sets `deleted = true` and saves. No document is removed from MongoDB. Every query carries `AndDeletedFalse` so deleted records are invisible through the API but remain available for audit if queried directly.

---

## Data Models

### `users` collection

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | Auto-generated |
| `name` | string | Required |
| `email` | string | Required, unique index |
| `password` | string | BCrypt-hashed, not returned in responses |
| `role` | VIEWER \| ANALYST \| ADMIN | Required |
| `active` | boolean | Default true |
| `createdAt` | datetime | Set on creation |

### `records` collection

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | Auto-generated |
| `amount` | double | Required, must be > 0 |
| `type` | INCOME \| EXPENSE | Required |
| `category` | string | Required |
| `date` | LocalDate | Optional (yyyy-MM-dd) |
| `notes` | string | Optional |
| `userId` | string | ID of the admin who created it |
| `deleted` | boolean | Default false — soft delete flag |

`date` is optional — entries like adjustments may not have a meaningful date. Dateless records count in totals but are excluded from `monthlyTrends`.

---

## Role Permission Matrix

| Action | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| Login | ✓ | ✓ | ✓ |
| View dashboard summary | ✓ | ✓ | ✓ |
| Paginated record listing | — | ✓ | ✓ |
| List / filter records | — | ✓ | ✓ |
| Create record | — | — | ✓ |
| Update / delete record | — | — | ✓ |
| Manage users (list, get, update) | — | — | ✓ |

---

## Setup

**Prerequisites:** Java 17+, MongoDB running on port 27017 (or a MongoDB Atlas URI)

```bash
# 1. Enter the project directory
cd finance

# 2. Set MongoDB URI (defaults to localhost if not set)
export MONGO_URI_FINANCE=mongodb://localhost:27017/finance_db
# Atlas: export MONGO_URI_FINANCE=mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/finance_db

# 3. Run (no local Maven install needed)
./mvnw spring-boot:run
# Server starts at http://localhost:8080

# 4. Swagger UI (optional)
# http://localhost:8080/swagger-ui.html
```

---

## API Reference

### Auth

#### POST /api/auth/login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "arjun@zorvyn.com", "password": "pass123"}'
# → "eyJhbGci..."   use this JWT as: Authorization: Bearer <token>
```

---

### Users

`POST /api/users` is public — no token needed (bootstraps the first admin).  
All other user endpoints require `Authorization: Bearer <admin-token>`.

```bash
# Create user
POST /api/users
{ "name": "Arjun Admin", "email": "arjun@zorvyn.com", "password": "pass123", "role": "ADMIN" }

# List all users (admin only)
GET /api/users

# Get user by ID (admin only)
GET /api/users/{id}

# Update role / active status — partial update (admin only)
PATCH /api/users/{id}
{ "role": "ANALYST" }           # role only
{ "active": false }             # deactivate only
{ "role": "ADMIN", "active": true }   # both
```

---

### Financial Records

All record endpoints require `Authorization: Bearer <token>`.

```bash
# Create (admin only) → 201 Created
POST /api/records
{ "amount": 75000, "type": "INCOME", "category": "Salary", "date": "2025-01-15", "notes": "Jan salary" }

# List all non-deleted records (analyst+)
GET /api/records

# Full replace update (admin only) → 200
PUT /api/records/{id}
{ "amount": 4200, "type": "EXPENSE", "category": "Food", "date": "2025-02-22" }

# Soft delete (admin only) → 204 No Content
DELETE /api/records/{id}

# Filter — all params optional and combinable (analyst+)
GET /api/records/filter?type=INCOME&from=2025-01-01&to=2025-01-31
GET /api/records/filter?category=Salary
GET /api/records/filter?type=EXPENSE&category=Rent&from=2025-02-01&to=2025-02-28
GET /api/records/filter?search=sal     # case-insensitive partial match on category

# Paginated (any authenticated user) — sorted newest first
GET /api/records/paginated?page=0&size=10
# → { "data": [...], "page": 0, "size": 10, "total": 42 }
```

---

### Dashboard

```bash
# Full summary — any authenticated role
GET /api/dashboard/summary
Authorization: Bearer <token>
```

Response:
```json
{
  "totalIncome": 150000.0,
  "totalExpense": 18500.0,
  "netBalance": 131500.0,
  "categoryTotals": {
    "Salary": 150000.0,
    "Rent": 15000.0,
    "Food": 3500.0
  },
  "recentActivity": [
    { "id": "...", "type": "INCOME", "amount": 75000.0, "category": "Salary", "date": "2025-02-15" }
  ],
  "monthlyTrends": {
    "2025-01": 60000.0,
    "2025-02": 71500.0
  }
}
```

---

### Error responses

All errors return `{"error": "message"}`. Validation errors return a field map.

| Status | When |
|---|---|
| 400 | Validation failure or duplicate email |
| 401 | Missing/invalid/expired token or inactive account |
| 403 | Role not permitted for the action |
| 404 | User or record not found |
| 429 | Rate limit exceeded (>100 requests/IP) |

---

## Unit Tests

19 tests across two service classes. Run without a live MongoDB — Mockito mocks all repositories.

```bash
./mvnw test
# or specific classes:
./mvnw test -Dtest="UserServiceTest,FinancialRecordServiceTest"
```

**UserServiceTest (9 tests):** missing auth context → 401, inactive user → 401, non-admin on user management → 403, duplicate email → 400, valid create/update flows, admin resolves and returns user list.

**FinancialRecordServiceTest (10 tests):** VIEWER/ANALYST on create → 403, ADMIN create tags `userId` on record, VIEWER on list → 403, soft delete sets `deleted=true` without calling `deleteById`, filter routing validates each param combination hits the correct repository method, no-params filter → `findByDeletedFalse`.

---

## Assumptions & Tradeoffs

**Full JWT auth stack** — Login, token generation, HMAC-SHA256 signing, BCrypt password hashing all implemented. The secret key is hardcoded for assessment simplicity; production would read from an environment variable.

**Role checks in service layer, not controllers** — Enforcement is consistent regardless of how a service method is called. Two helpers (`assertAdmin`, `assertNotViewer`) keep checks to one-liners.

**Soft delete as the default** — Records are never permanently removed. All queries carry `AndDeletedFalse`. To convert to hard delete later: remove the flag from the entity and swap the query method names.

**All filter combinations are explicit** — 8 dedicated repository methods cover every param combination so no filter silently overrides another. `search` runs exclusively when present (quick-lookup, not a combinator).

**Unique email enforced at two levels** — MongoDB unique index + `existsByEmail` in the service. The service check ensures a clean 400 response rather than a raw MongoDB exception.

**In-memory dashboard aggregation** — Records are fetched and aggregated in Java streams. Simple and readable for typical dataset sizes. At scale this would move to MongoDB's aggregation pipeline.

**Rate limiting in-memory** — IP-based counter in `AuthFilter` (100 req/IP). Resets on restart. Production would use Redis or a dedicated rate-limiting library.

**`date` optional on records** — Covers entries like adjustments or opening balances. Dateless records count in totals but are excluded from `monthlyTrends`.

**`POST /api/users` is public** — Bootstrapping problem: someone has to create the first admin. All subsequent user management requires an admin token.