# Finance Dashboard Backend

A role-based finance dashboard backend built as a developer assessment. Covers JWT authentication, financial record management, role-based access control, MongoDB aggregation-powered analytics, soft delete, pagination, search, rate limiting, and full unit test coverage.

**Stack:** Java 17 · Spring Boot 3.2.4 · MongoDB · Spring Security · JJWT 0.11.5 · Lombok · JUnit 5 + Mockito · SpringDoc OpenAPI

---

## Table of Contents

- [Project Structure](#project-structure)
- [Architecture & Request Lifecycle](#architecture--request-lifecycle)
- [Authentication Flow](#authentication-flow)
- [Role Enforcement](#role-enforcement)
- [Data Models](#data-models)
- [Role Permission Matrix](#role-permission-matrix)
- [Setup & Running](#setup--running)
- [API Reference](#api-reference)
- [Dashboard Aggregation](#dashboard-aggregation)
- [Filter Routing](#filter-routing)
- [Unit Tests](#unit-tests)
- [Features Coverage](#features-coverage)
- [Assumptions & Tradeoffs](#assumptions--tradeoffs)

---

## Project Structure

```
src/main/java/com/zorvyn/finance/
│
├── controller/
│   ├── AuthController.java              POST /api/auth/login
│   ├── UserController.java              CRUD /api/users
│   ├── FinancialRecordController.java   CRUD + filter /api/records
│   └── DashboardController.java         GET /api/dashboard/summary
│
├── service/
│   ├── UserService.java                 User management, caller resolution
│   ├── FinancialRecordService.java      Record CRUD, filtering, pagination
│   └── DashboardService.java            Aggregated summary computation
│
├── repository/
│   ├── UserRepository.java
│   ├── FinancialRecordRepository.java       8 filter methods + pagination
│   ├── FinancialRecordCustomRepository.java Aggregation interface
│   └── FinancialRecordCustomRepositoryImpl.java  MongoDB aggregation pipeline
│
├── entity/
│   ├── User.java                        users collection
│   ├── FinancialRecord.java             records collection
│   ├── Role.java                        Enum: VIEWER, ANALYST, ADMIN
│   └── RecordType.java                  Enum: INCOME, EXPENSE
│
├── dto/
│   ├── LoginRequest.java                { email, password }
│   ├── UserUpdateRequest.java           { role?, active? }
│   ├── DashboardSummary.java            Summary response
│   └── PageResponse.java                Generic paginated wrapper
│
├── security/
│   ├── AuthFilter.java                  JWT validation + rate limiting (OncePerRequestFilter)
│   ├── AuthContext.java                 ThreadLocal for current userId
│   ├── JwtUtil.java                     HMAC-SHA256 token generation & validation
│   └── SecurityConfig.java             Spring Security filter chain
│
└── exception/
    ├── GlobalExceptionHandler.java      @ControllerAdvice — maps all exceptions to HTTP
    ├── AccessDeniedException.java       403
    ├── UnauthorizedException.java       401
    └── ResourceNotFoundException.java  404
```

**Separation of concerns:** Controllers parse HTTP only. Services hold all business logic and role checks. Repositories contain no logic. This makes each layer independently testable.

---

## Architecture & Request Lifecycle

```
Incoming HTTP Request
        │
        ▼
┌───────────────────────────────────────────┐
│  AuthFilter  (OncePerRequestFilter)       │
│  1. Rate limit: count per IP → 429 if >100│
│  2. Extract Bearer token from header      │
│  3. JwtUtil.extractUserId(token)          │
│  4. JwtUtil.extractRole(token)            │
│  5. AuthContext.set(userId) → ThreadLocal │
│  6. Populate Spring SecurityContext       │
│  finally: AuthContext.clear()  ← no leaks│
└──────────────────┬────────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────┐
│  SecurityConfig                           │
│  /api/auth/** → permitAll                 │
│  POST /api/users → permitAll              │
│  everything else → authenticated          │
└──────────────────┬────────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────┐
│  Controller                               │
│  @PreAuthorize("hasRole('ADMIN')")        │
│  Parses params/body → calls service       │
└──────────────────┬────────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────┐
│  Service                                  │
│  resolveCaller() → load user, check active│
│  assertAdmin() / assertNotViewer()        │
│  Execute business logic                   │
└──────────────────┬────────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────┐
│  Repository  (Spring Data MongoDB)        │
│  Executes query → returns result          │
└──────────────────┬────────────────────────┘
                   │
                   ▼
         JSON Response to Client
                   │
                   ▼  (on any exception)
┌───────────────────────────────────────────┐
│  GlobalExceptionHandler                   │
│  Maps exception → HTTP status + JSON body │
└───────────────────────────────────────────┘
```

---

## Authentication Flow

```
POST /api/auth/login   { "email": "...", "password": "..." }
        │
        ▼
  userRepository.findByEmail(email)         → 404 if not found
  BCryptPasswordEncoder.matches(raw, hash)  → 400 if mismatch
  JwtUtil.generateToken(userId, role)
        │
        ▼
  JWT: { sub: "<mongoUserId>", role: "ADMIN", iat: ... }
  Signed with HMAC-SHA256
        │
        ▼
  Returns raw JWT string
  Client sends: Authorization: Bearer <token>
```

**Token generation:**
```java
// JwtUtil.java
public String generateToken(String userId, String role) {
    return Jwts.builder()
            .setSubject(userId)
            .claim("role", role)
            .setIssuedAt(new Date())
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
            .compact();
}
```

`POST /api/users` is public — required to bootstrap the first admin. After that, all management needs an admin token.

---

## Role Enforcement

Role checks are intentionally enforced at **two layers**:

**Layer 1 — `@PreAuthorize` on controllers** catches unauthorized calls before they reach service logic:
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<FinancialRecord> create(...) { ... }

@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
@GetMapping
public List<FinancialRecord> getAll() { ... }
```

**Layer 2 — explicit guards in services** ensure enforcement even when a service is called internally:
```java
public FinancialRecord createRecord(FinancialRecord record) {
    User caller = userService.resolveCaller();
    assertAdmin(caller);   // throws 403 if not ADMIN
    record.setUserId(caller.getId());
    record.setDeleted(false);
    return recordRepository.save(record);
}
```

`resolveCaller()` also checks the `active` flag — a deactivated account is blocked on its very next request:
```java
public User resolveCaller() {
    String callerId = AuthContext.get();
    if (callerId == null) throw new UnauthorizedException("Missing auth");
    User caller = userRepository.findById(callerId)
            .orElseThrow(() -> new ResourceNotFoundException("Caller not found"));
    if (!caller.isActive()) throw new UnauthorizedException("Account is inactive");
    return caller;
}
```

---

## Data Models

### `users` collection

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | Auto-generated |
| `name` | String | `@NotBlank` |
| `email` | String | `@Email`, unique index |
| `password` | String | BCrypt-hashed, never returned in responses |
| `role` | VIEWER \| ANALYST \| ADMIN | `@NotNull` |
| `active` | boolean | Default `true` |
| `createdAt` | LocalDateTime | Set on creation |

### `records` collection

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | Auto-generated |
| `amount` | Double | `@NotNull`, `@Positive` |
| `type` | INCOME \| EXPENSE | `@NotNull` |
| `category` | String | `@NotBlank` |
| `date` | LocalDate | Optional (yyyy-MM-dd) |
| `notes` | String | Optional |
| `userId` | String | Admin ID who created the record |
| `deleted` | boolean | Default `false`, `@JsonIgnore` — hidden from responses |
| `createdAt` | LocalDateTime | Spring Data `@CreatedDate` |
| `updatedAt` | LocalDateTime | Spring Data `@LastModifiedDate` |

`date` is optional — entries like adjustments may have no date. Dateless records count in totals but are excluded from `monthlyTrends`.

---

## Role Permission Matrix

| Action | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| Login | ✓ | ✓ | ✓ |
| Dashboard summary | ✓ | ✓ | ✓ |
| List / filter / paginate records | — | ✓ | ✓ |
| Create record | — | — | ✓ |
| Update record | — | — | ✓ |
| Soft delete record | — | — | ✓ |
| List / get / update users | — | — | ✓ |

---

## Setup & Running

**Prerequisites:** Java 17+, MongoDB on `localhost:27017` (or Atlas URI)

```bash
# 1. Clone and enter the project
git clone https://github.com/pranavsaai/finance_dashboard_backend.git
cd finance_dashboard_backend

# 2. Set MongoDB URI (defaults to localhost if not set)
export MONGO_URI_FINANCE=mongodb://localhost:27017/finance_db

# Atlas:
# export MONGO_URI_FINANCE=mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/finance_db

# 3. Run (no local Maven needed — wrapper included)
./mvnw spring-boot:run

# Server:    http://localhost:8080
# Swagger:   http://localhost:8080/swagger-ui.html
```

**application.properties:**
```properties
spring.data.mongodb.uri=${MONGO_URI_FINANCE:mongodb://localhost:27017/finance_db}
spring.application.name=finance
springdoc.swagger-ui.path=/swagger-ui.html
```

**Run tests:**
```bash
./mvnw test
```

---

## API Reference

All endpoints except `POST /api/auth/login` and `POST /api/users` require:
```
Authorization: Bearer <jwt-token>
```

---

### Auth

#### `POST /api/auth/login` — Login

**Access:** Public

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "arjun@zorvyn.com", "password": "pass123"}'
```

**200 OK** — returns raw JWT string:
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NjFl...
```

**Error responses:**
```json
{ "error": "User not found" }      // 404
{ "error": "Invalid password" }    // 400
```

---

### Users

#### `POST /api/users` — Create user

**Access:** Public (bootstrap first admin, then manage via admin token)

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Arjun Admin", "email": "arjun@zorvyn.com", "password": "pass123", "role": "ADMIN"}'
```

**201 Created:**
```json
{
  "id": "661e8a3f2c4b1a0012abcdef",
  "name": "Arjun Admin",
  "email": "arjun@zorvyn.com",
  "role": "ADMIN",
  "active": true,
  "createdAt": "2025-01-15T10:30:00"
}
```
> `password` is never returned in any response.

**400 — validation failure:**
```json
{
  "name": "Name is required",
  "email": "Provide a valid email address"
}
```
**400 — duplicate email:**
```json
{ "error": "A user with this email already exists" }
```

---

#### `GET /api/users` — List all users

**Access:** ADMIN

```bash
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer <admin-token>"
```

**200 OK:** Array of user objects.

---

#### `GET /api/users/{id}` — Get user by ID

**Access:** ADMIN

```bash
curl http://localhost:8080/api/users/661e8a3f2c4b1a0012abcdef \
  -H "Authorization: Bearer <admin-token>"
```

**404:**
```json
{ "error": "User not found with id: 661e8a3f2c4b1a0012abcdef" }
```

---

#### `PATCH /api/users/{id}` — Update role or status

**Access:** ADMIN — all fields optional, only provided fields are applied.

```bash
# Change role only
curl -X PATCH http://localhost:8080/api/users/661e8b4a3d5c2b1123ghijkl \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"role": "ANALYST"}'

# Deactivate only
curl -X PATCH http://localhost:8080/api/users/661e8b4a3d5c2b1123ghijkl \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"active": false}'
```

**200 OK:** Updated user object.

---

### Financial Records

#### `POST /api/records` — Create record

**Access:** ADMIN

```bash
curl -X POST http://localhost:8080/api/records \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 75000, "type": "INCOME", "category": "Salary", "date": "2025-01-15", "notes": "January salary"}'
```

`date` and `notes` are optional. `type` must be `INCOME` or `EXPENSE`.

**201 Created:**
```json
{
  "id": "661f1a2b3c4d5e6f7a8b9c0d",
  "amount": 75000.0,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-01-15",
  "notes": "January salary",
  "userId": "661e8a3f2c4b1a0012abcdef",
  "createdAt": "2025-01-15T14:00:00",
  "updatedAt": "2025-01-15T14:00:00"
}
```

**400 — validation failure:**
```json
{
  "amount": "Amount must be greater than zero",
  "category": "Category is required"
}
```

---

#### `GET /api/records` — List all records

**Access:** ANALYST, ADMIN

```bash
curl http://localhost:8080/api/records \
  -H "Authorization: Bearer <token>"
```

**200 OK:** Array of all non-deleted records.

---

#### `PUT /api/records/{id}` — Update record

**Access:** ADMIN — full field replacement (id and userId are preserved).

```bash
curl -X PUT http://localhost:8080/api/records/661f1a2b3c4d5e6f7a8b9c0d \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 4200, "type": "EXPENSE", "category": "Rent", "date": "2025-02-01"}'
```

**200 OK:** Updated record object.

---

#### `DELETE /api/records/{id}` — Soft delete record

**Access:** ADMIN — sets `deleted = true`, never removes from MongoDB.

```bash
curl -X DELETE http://localhost:8080/api/records/661f1a2b3c4d5e6f7a8b9c0d \
  -H "Authorization: Bearer <admin-token>"
```

**204 No Content.**

---

#### `GET /api/records/filter` — Filter records

**Access:** ANALYST, ADMIN — all params optional, all combinable.

| Param | Type | Notes |
|---|---|---|
| `type` | `INCOME` \| `EXPENSE` | Filter by record type |
| `category` | string | Exact match |
| `from` | yyyy-MM-dd | Must be paired with `to` |
| `to` | yyyy-MM-dd | Must be paired with `from` |
| `search` | string | Case-insensitive match on category + notes. Overrides other params. |

```bash
# By type + date range
curl "http://localhost:8080/api/records/filter?type=INCOME&from=2025-01-01&to=2025-01-31" \
  -H "Authorization: Bearer <token>"

# All three structured filters
curl "http://localhost:8080/api/records/filter?type=EXPENSE&category=Rent&from=2025-02-01&to=2025-02-28" \
  -H "Authorization: Bearer <token>"

# Keyword search
curl "http://localhost:8080/api/records/filter?search=sal" \
  -H "Authorization: Bearer <token>"
```

**400 — bad date range:**
```json
{ "error": "From date cannot be after To date" }
```

---

#### `GET /api/records/paginated` — Paginated listing

**Access:** ANALYST, ADMIN — sorted newest first by date.

| Param | Default |
|---|---|
| `page` | `0` |
| `size` | `10` |

```bash
curl "http://localhost:8080/api/records/paginated?page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

**200 OK:**
```json
{
  "data": [ { "id": "...", "amount": 75000.0, "type": "INCOME", "category": "Salary", "date": "2025-03-01" } ],
  "page": 0,
  "size": 10,
  "total": 42
}
```

---

### Dashboard

#### `GET /api/dashboard/summary` — Full summary

**Access:** All authenticated roles (VIEWER, ANALYST, ADMIN)

```bash
curl http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer <any-valid-token>"
```

**200 OK:**
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
    { "id": "...", "type": "INCOME", "amount": 75000.0, "category": "Salary", "date": "2025-03-01" }
  ],
  "monthlyTrends": {
    "2025-01": 60000.0,
    "2025-02": 71500.0,
    "2025-03": -500.0
  }
}
```

`monthlyTrends` values are net per month (income − expenses). Negative means expenses exceeded income that month. Sorted chronologically via `TreeMap`.

---

### Error Responses

All errors return a consistent envelope:
```json
{ "error": "descriptive message" }
```

Validation errors return a field map:
```json
{ "amount": "Amount must be greater than zero", "category": "Category is required" }
```

| Status | When |
|---|---|
| 400 | Validation failure, duplicate email, invalid date range |
| 401 | Missing/invalid token, inactive account |
| 403 | Role not permitted for this action |
| 404 | User or record not found |
| 429 | More than 100 requests from the same IP |
| 500 | Unexpected error (fallback handler) |

---

## Dashboard Aggregation

Dashboard totals and trends are computed using **MongoDB's aggregation pipeline** — not Java streams. This means the database does the heavy lifting, not application memory.

```
DashboardService.getSummary()
│
├── getTotalIncome()
│   match(type=INCOME, deleted=false) → group() → sum(amount)
│
├── getTotalExpense()
│   match(type=EXPENSE, deleted=false) → group() → sum(amount)
│
├── netBalance = totalIncome - totalExpense
│
├── getCategoryTotals()
│   match(deleted=false) → group(category) → sum(amount)
│   Returns Map<String, Double>
│
├── getMonthlyTrends()
│   project(year, month, amount, type)
│   → group(year+month)
│   → conditional sum: INCOME positive, EXPENSE negative
│   Returns TreeMap<"YYYY-MM", Double>  ← TreeMap sorts chronologically
│
└── findTop5ByDeletedFalseOrderByDateDesc()
    → recentActivity (last 5 records)
```

All aggregations use `deleted=false` — soft-deleted records never appear in dashboard numbers.

---

## Filter Routing

`GET /api/records/filter` routes to one of 8 explicit Spring Data query methods depending on which params are present. No combination silently overrides another:

```
search present?
  └── findByCategoryContainingIgnoreCaseOrNotesContainingIgnoreCaseAndDeletedFalse()

type + category + dateRange → findByTypeAndCategoryAndDateBetweenAndDeletedFalse()
type + dateRange            → findByTypeAndDateBetweenAndDeletedFalse()
category + dateRange        → findByCategoryAndDateBetweenAndDeletedFalse()
type + category             → findByTypeAndCategoryAndDeletedFalse()
dateRange only              → findByDateBetweenAndDeletedFalse()
type only                   → findByTypeAndDeletedFalse()
category only               → findByCategoryAndDeletedFalse()
no params                   → findByDeletedFalse()
```

Spring Data generates the actual MongoDB queries from these method names. Every method has `AndDeletedFalse` — soft-deleted records are always excluded.

---

## Unit Tests

19 tests across two service classes. No live MongoDB needed — all repositories mocked with Mockito.

```bash
./mvnw test
# Expected: Tests run: 19, Failures: 0, Errors: 0
```

### UserServiceTest (9 tests)

| Test | Verifies |
|---|---|
| `resolveCaller_missingHeader` | No auth context → 401 |
| `resolveCaller_inactiveUser` | Inactive account → 401 |
| `resolveCaller_activeUser` | Valid context → correct user returned |
| `getAllUsers_viewerCaller` | VIEWER on user list → 403 |
| `getAllUsers_adminCaller` | ADMIN can list users |
| `updateUser_changeRole` | Admin changes VIEWER → ANALYST |
| `updateUser_deactivate` | Admin sets `active = false` |
| `createUser_duplicateEmail` | Duplicate email → 400 |
| `createUser_newEmail` | Valid creation saves and returns user |

### FinancialRecordServiceTest (10 tests)

| Test | Verifies |
|---|---|
| `createRecord_viewerCannot` | VIEWER create → 403 |
| `createRecord_analystCannot` | ANALYST create → 403 |
| `createRecord_adminCan` | ADMIN creates, `userId` tagged on record |
| `getAllRecords_viewerCannot` | VIEWER list → 403 |
| `getAllRecords_analystCan` | ANALYST can list |
| `deleteRecord_notFound` | Unknown ID → 404 |
| `deleteRecord_setsDeletedTrue` | Soft delete sets flag, never calls `deleteById` |
| `filterRecords_typeAndDateRange` | Routes to correct combined repository method |
| `filterRecords_keywordSearch` | `search` param uses keyword method |
| `filterRecords_noParams` | No params → `findByDeletedFalse()` |
| `filterRecords_viewerCannot` | VIEWER on filter → 403 |
| `getPaginated_viewerCannot` | VIEWER on paginated → 403 |

---

## Features Coverage

### Core Requirements

| Requirement | Status |
|---|:---:|
| User creation and management | ✓ |
| Role assignment (VIEWER / ANALYST / ADMIN) | ✓ |
| Active / inactive user status | ✓ |
| Role-based access restrictions | ✓ |
| Financial record CRUD | ✓ |
| Record filtering (date, category, type) | ✓ |
| Dashboard summary APIs | ✓ |
| Input validation + error responses | ✓ |
| Correct HTTP status codes | ✓ |
| Data persistence (MongoDB) | ✓ |

### Optional Enhancements

| Enhancement | Status |
|---|:---:|
| JWT authentication (HMAC-SHA256 + BCrypt) | ✓ |
| Pagination with total count | ✓ |
| Keyword search on category + notes | ✓ |
| Soft delete (audit-safe) | ✓ |
| Rate limiting (100 req/IP, in-memory) | ✓ |
| Unit tests (19, no live DB needed) | ✓ |
| Swagger UI at `/swagger-ui.html` | ✓ |

---

## Assumptions & Tradeoffs

**JWT secret is secured** — lives in `JwtUtil.java` for assessment simplicity. Production reads from an environment variable. Tokens have expiry set by adding `.setExpiration(...)`.

**Dual-layer role enforcement** — `@PreAuthorize` on controllers + `assertAdmin()`/`assertNotViewer()` in services. Redundancy is intentional: the service layer guarantees enforcement even if called internally or if a controller annotation is misconfigured.

**MongoDB aggregation pipeline for dashboard** — totals and trends are computed in the database, not in Java memory. This scales correctly for large datasets.

**8 explicit filter methods** — every param combination is a named repository method. Verbose but completely predictable — one filter can never silently override another.

**Unique email at two levels** — MongoDB unique index prevents DB-level duplicates; `existsByEmail()` in the service returns a clean 400 rather than a raw `DuplicateKeyException`.

**In-memory rate limiting** — `ConcurrentHashMap<IP, count>` in `AuthFilter`. Resets on restart. Production would use Redis for distributed, persistent limiting.

**Soft delete by default** — records are never removed. Every query carries `AndDeletedFalse`. Historical data stays in MongoDB for auditing. Converting to hard delete would just mean removing the flag and renaming the query methods.

**`date` optional on records** — covers entries like adjustments with no meaningful date. These count in totals but are excluded from `monthlyTrends` since there is no month to group by.

**`POST /api/users` is public** — necessary to bootstrap the first admin account without a chicken-and-egg problem. All subsequent user management requires an admin token.tstrap the first admin account without a chicken-and-egg problem. All subsequent user management requires an admin token.
