# Finance Dashboard Backend

A role-based finance dashboard backend built as a developer assessment for Zorvyn. Covers financial record management, user roles, JWT-based access control, dashboard analytics, and more.

**Stack:** Java 17 · Spring Boot 3.2.4 · MongoDB · JWT (jjwt 0.11.5) · Spring Security · Lombok · JUnit 5 + Mockito · SpringDoc OpenAPI (Swagger)

---

## Table of Contents

- [Project Structure](#project-structure)
- [Features — Complete Coverage](#features--complete-coverage)
- [Architecture & How It Works](#architecture--how-it-works)
  - [1. Authentication Flow](#1-authentication-flow)
  - [2. Request Lifecycle](#2-request-lifecycle)
  - [3. Role Enforcement](#3-role-enforcement)
  - [4. Dashboard Aggregation](#4-dashboard-aggregation)
  - [5. Filter Routing](#5-filter-routing)
  - [6. Soft Delete](#6-soft-delete)
  - [7. Rate Limiting](#7-rate-limiting)
- [Data Models](#data-models)
- [Role Permission Matrix](#role-permission-matrix)
- [Setup & Running](#setup--running)
- [API Documentation](#api-documentation)
  - [Auth](#auth)
  - [Users](#users)
  - [Financial Records](#financial-records)
  - [Dashboard](#dashboard)
  - [Error Responses](#error-responses)
- [Unit Tests](#unit-tests)
- [Optional Enhancements Implemented](#optional-enhancements-implemented)
- [Assumptions & Tradeoffs](#assumptions--tradeoffs)

---

## Project Structure

```
src/main/java/com/zorvyn/finance/
│
├── controller/           HTTP layer — routes requests, delegates to services
│   ├── AuthController.java           POST /api/auth/login
│   ├── UserController.java           CRUD for /api/users
│   ├── FinancialRecordController.java CRUD + filter for /api/records
│   └── DashboardController.java      GET /api/dashboard/summary
│
├── service/              Business logic + role enforcement
│   ├── UserService.java              User management, caller resolution
│   ├── FinancialRecordService.java   Record CRUD, filtering, pagination
│   └── DashboardService.java         Aggregated summary computation
│
├── repository/           Data access layer
│   ├── UserRepository.java                         Spring Data MongoDB
│   ├── FinancialRecordRepository.java              8 filter methods + pagination
│   ├── FinancialRecordCustomRepository.java        Aggregation interface
│   └── FinancialRecordCustomRepositoryImpl.java    MongoDB aggregation pipeline
│
├── entity/               MongoDB document models
│   ├── User.java                     users collection
│   ├── FinancialRecord.java          records collection
│   ├── Role.java                     Enum: VIEWER, ANALYST, ADMIN
│   └── RecordType.java               Enum: INCOME, EXPENSE
│
├── dto/                  Request/response shapes
│   ├── LoginRequest.java             { email, password }
│   ├── UserUpdateRequest.java        { role?, active? } — partial update
│   ├── DashboardSummary.java         Summary response with all aggregates
│   └── PageResponse.java             Generic paginated wrapper
│
├── security/             Auth infrastructure
│   ├── AuthFilter.java               JWT extraction + rate limiting (OncePerRequestFilter)
│   ├── AuthContext.java              ThreadLocal storage for current userId
│   ├── JwtUtil.java                  HMAC-SHA256 token generation and validation
│   └── SecurityConfig.java          Spring Security filter chain configuration
│
└── exception/            Error handling
    ├── GlobalExceptionHandler.java   @ControllerAdvice — maps exceptions to HTTP responses
    ├── AccessDeniedException.java    Custom 403
    ├── UnauthorizedException.java    Custom 401
    └── ResourceNotFoundException.java Custom 404
```

Each layer has a single responsibility. Controllers do not touch the database. Services do not know about HTTP. Repositories contain no business logic.

---

## Features — Complete Coverage

### Core Requirements

| Requirement | Status | Implementation |
|---|:---:|---|
| User creation and management | ✅ | `UserController` + `UserService` |
| Role assignment (VIEWER/ANALYST/ADMIN) | ✅ | `Role` enum, `User.role` field |
| User active/inactive status | ✅ | `User.active` flag, enforced in `resolveCaller()` |
| Role-based restrictions | ✅ | `@PreAuthorize` + service-layer guards |
| Financial record CRUD | ✅ | `FinancialRecordController` + `FinancialRecordService` |
| Record filtering (date, category, type) | ✅ | 8 dedicated repository methods + keyword search |
| Dashboard summary APIs | ✅ | MongoDB aggregation pipeline in `DashboardService` |
| Access control enforcement | ✅ | Dual-layer: `@PreAuthorize` + `assertAdmin()`/`assertNotViewer()` |
| Input validation | ✅ | `@Valid`, `@NotBlank`, `@Email`, `@Positive` |
| Useful error responses | ✅ | `GlobalExceptionHandler` with `{"error": "..."}` format |
| Appropriate HTTP status codes | ✅ | 201/200/204/400/401/403/404/429/500 |
| Data persistence (MongoDB) | ✅ | Spring Data MongoDB, unique indexes |

### Optional Enhancements

| Enhancement | Status | Implementation |
|---|:---:|---|
| JWT authentication | ✅ | HMAC-SHA256 signed tokens, BCrypt password hashing |
| Pagination | ✅ | `GET /api/records/paginated?page=0&size=10` |
| Search support | ✅ | Case-insensitive keyword match on category + notes |
| Soft delete | ✅ | `deleted = true` flag, never removes from DB |
| Rate limiting | ✅ | In-memory 100 req/IP counter in `AuthFilter` |
| Unit tests | ✅ | 19 tests, JUnit 5 + Mockito, no live MongoDB needed |
| API documentation | ✅ | Swagger UI via SpringDoc at `/swagger-ui.html` |

---

## Architecture & How It Works

### 1. Authentication Flow

```
POST /api/auth/login  { "email": "...", "password": "..." }
         │
         ▼
  AuthController
  ├─ userRepository.findByEmail(email)       → 404 if not found
  ├─ BCryptPasswordEncoder.matches(raw, hash) → 400 if mismatch
  └─ JwtUtil.generateToken(userId, role)
         │
         ▼
  JWT payload:
    { "sub": "<mongoUserId>", "role": "ADMIN", "iat": ... }
  Signed with HMAC-SHA256 using a 32-byte secret key
         │
         ▼
  Returns raw JWT string — client sends as:
  Authorization: Bearer <token>
```

**Code reference — token generation:**

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

---

### 2. Request Lifecycle

Every authenticated request flows through these layers in order:

```
Incoming HTTP Request
        │
        ▼
  AuthFilter  (OncePerRequestFilter)
  ├─ Rate limit: count requests per IP → 429 if > 100
  ├─ Read Authorization header → extract Bearer token
  ├─ JwtUtil.extractUserId(token) → validate signature
  ├─ JwtUtil.extractRole(token) → extract role claim
  ├─ AuthContext.set(userId) → store in ThreadLocal
  └─ Set Spring SecurityContext with role authority
        │
        ▼
  SecurityConfig
  ├─ /api/auth/** → permitAll (no token required)
  ├─ POST /api/users → permitAll (bootstrap first admin)
  └─ Everything else → must be authenticated
        │
        ▼
  @PreAuthorize annotation on controller method
  e.g. hasRole('ADMIN'), hasAnyRole('ADMIN','ANALYST')
        │
        ▼
  Controller
  Parses request params/body, delegates to service
        │
        ▼
  Service
  ├─ AuthContext.get() → get userId from ThreadLocal
  ├─ userRepository.findById(userId) → load User
  ├─ assertAdmin() or assertNotViewer() → role check
  └─ Run business logic
        │
        ▼
  Repository
  Spring Data MongoDB executes query
        │
        ▼
  Response JSON returned to client
        │
        ▼
  AuthFilter finally block: AuthContext.clear() ← prevents ThreadLocal leaks
```

---

### 3. Role Enforcement

Role checks happen in **two places** intentionally:

**Layer 1 — Controller (@PreAuthorize):**
```java
// FinancialRecordController.java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<FinancialRecord> create(@Valid @RequestBody FinancialRecord record) {
    return new ResponseEntity<>(recordService.createRecord(record), HttpStatus.CREATED);
}

@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
@GetMapping
public List<FinancialRecord> getAll() {
    return recordService.getAllRecords();
}
```

**Layer 2 — Service (explicit role check):**
```java
// FinancialRecordService.java
public FinancialRecord createRecord(FinancialRecord record) {
    User caller = userService.resolveCaller(); // fetches from DB, checks active
    assertAdmin(caller);                        // throws 403 if not ADMIN
    record.setUserId(caller.getId());
    record.setDeleted(false);
    return recordRepository.save(record);
}

private void assertAdmin(User user) {
    if (user.getRole() != Role.ADMIN) {
        throw new AccessDeniedException("Only ADMIN can perform this action");
    }
}

private void assertNotViewer(User user) {
    if (user.getRole() == Role.VIEWER) {
        throw new AccessDeniedException("VIEWER role does not have access to records");
    }
}
```

`resolveCaller()` also checks the `active` flag — a deactivated account is rejected on the very next request:

```java
// UserService.java
public User resolveCaller() {
    String callerId = AuthContext.get();
    if (callerId == null || callerId.isBlank()) {
        throw new UnauthorizedException("Missing X-User-Id header");
    }
    User caller = userRepository.findById(callerId)
            .orElseThrow(() -> new ResourceNotFoundException("Caller user not found"));
    if (!caller.isActive()) {
        throw new UnauthorizedException("Account is inactive");  // 401
    }
    return caller;
}
```

---

### 4. Dashboard Aggregation

```
GET /api/dashboard/summary
        │
        ▼
  DashboardService.getSummary()
  ├─ resolveCaller()                  → any active user (all roles allowed)
  │
  ├─ recordRepository.getTotalIncome()
  │   MongoDB aggregation:
  │   match(type=INCOME, deleted=false) → group() → sum(amount)
  │
  ├─ recordRepository.getTotalExpense()
  │   MongoDB aggregation:
  │   match(type=EXPENSE, deleted=false) → group() → sum(amount)
  │
  ├─ totalIncome - totalExpense        → netBalance
  │
  ├─ recordRepository.getCategoryTotals()
  │   MongoDB aggregation:
  │   match(deleted=false) → group(category) → sum(amount)
  │   Returns Map<String, Double>
  │
  ├─ recordRepository.getMonthlyTrends()
  │   MongoDB aggregation:
  │   project(year, month, amount, type)
  │   → group(year+month) → conditional sum (INCOME positive, EXPENSE negative)
  │   Returns TreeMap<"YYYY-MM", Double> (sorted chronologically)
  │
  └─ recordRepository.findTop5ByDeletedFalseOrderByDateDesc()
      → Last 5 records as recentActivity
```

**Code reference — MongoDB aggregation for monthly trends:**

```java
// FinancialRecordCustomRepositoryImpl.java
public Map<String, Double> getMonthlyTrends() {
    Aggregation agg = Aggregation.newAggregation(
        Aggregation.project()
            .andExpression("year(date)").as("year")
            .andExpression("month(date)").as("month")
            .and("amount").as("amount")
            .and("type").as("type"),
        Aggregation.group("year", "month")
            .sum(
                ConditionalOperators.when(Criteria.where("type").is("INCOME"))
                    .thenValueOf("amount")
                    .otherwise(
                        ArithmeticOperators.Multiply.valueOf("amount").multiplyBy(-1)
                    )
            ).as("total")
    );
    // ... Returns TreeMap for automatic chronological sorting
}
```

`TreeMap` is used so months sort chronologically without extra logic.

---

### 5. Filter Routing

`GET /api/records/filter` accepts `type`, `category`, `from`, `to`, and `search` — all optional, all combinable.

When `search` is provided it runs a case-insensitive keyword match against category and notes, ignoring other params. Otherwise, the service routes to one of 8 explicit Spring Data query methods so no filter combination ever silently overrides another:

```java
// FinancialRecordService.java — filterRecords()
if (search != null && !search.isBlank()) {
    return recordRepository
        .findByCategoryContainingIgnoreCaseOrNotesContainingIgnoreCaseAndDeletedFalse(keyword, keyword);
}

// Then for structured filters:
if (hasType && hasCategory && hasDateRange)
    → findByTypeAndCategoryAndDateBetweenAndDeletedFalse()
if (hasType && hasDateRange)
    → findByTypeAndDateBetweenAndDeletedFalse()
if (hasCategory && hasDateRange)
    → findByCategoryAndDateBetweenAndDeletedFalse()
if (hasType && hasCategory)
    → findByTypeAndCategoryAndDeletedFalse()
if (hasDateRange)
    → findByDateBetweenAndDeletedFalse()
if (hasType)
    → findByTypeAndDeletedFalse()
if (hasCategory)
    → findByCategoryAndDeletedFalse()
// no params:
    → findByDeletedFalse()
```

Spring Data generates actual MongoDB queries from method names. Every method has `AndDeletedFalse` — soft-deleted records are always excluded.

---

### 6. Soft Delete

```java
// FinancialRecordService.java
public void deleteRecord(String id) {
    User caller = userService.resolveCaller();
    assertAdmin(caller);

    FinancialRecord record = recordRepository.findById(id)
            .filter(r -> !r.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));

    record.setDeleted(true);      // marks as deleted — never removes from MongoDB
    recordRepository.save(record);
}
```

`DELETE /api/records/{id}` sets `deleted = true` and saves. No document is removed from MongoDB. Every query carries `AndDeletedFalse` so deleted records are invisible through all APIs but remain in the database for audit purposes.

---

### 7. Rate Limiting

In-memory per-IP counter lives in `AuthFilter`. Counter increments on every request; at > 100 it returns HTTP 429 immediately before any other processing:

```java
// AuthFilter.java
private static final Map<String, Integer> requestCount = new ConcurrentHashMap<>();

String ip = request.getRemoteAddr();
requestCount.put(ip, requestCount.getOrDefault(ip, 0) + 1);

if (requestCount.get(ip) > 100) {
    response.setStatus(429);
    response.getWriter().write("Too many requests");
    return;
}
```

Counter resets on server restart. Production would use Redis for distributed, persistent rate limiting.

---

## Data Models

### `users` collection

| Field | Type | Constraints |
|---|---|---|
| `_id` | ObjectId | Auto-generated |
| `name` | String | `@NotBlank` |
| `email` | String | `@Email`, `@NotBlank`, unique index |
| `password` | String | `@NotBlank`, BCrypt-hashed before storage |
| `role` | VIEWER \| ANALYST \| ADMIN | `@NotNull` |
| `active` | boolean | Default `true` |
| `createdAt` | LocalDateTime | Set on creation (`LocalDateTime.now()`) |

### `records` collection (mapped to `financialRecord` in MongoDB)

| Field | Type | Constraints |
|---|---|---|
| `_id` | ObjectId | Auto-generated |
| `amount` | Double | `@NotNull`, `@Positive` (must be > 0) |
| `type` | INCOME \| EXPENSE | `@NotNull` |
| `category` | String | `@NotBlank` |
| `date` | LocalDate | Optional (format: `yyyy-MM-dd`) |
| `notes` | String | Optional |
| `userId` | String | Set to the creating admin's ID |
| `deleted` | boolean | Default `false`, hidden from API responses (`@JsonIgnore`) |
| `createdAt` | LocalDateTime | `@CreatedDate` (Spring Data auditing) |
| `updatedAt` | LocalDateTime | `@LastModifiedDate` (Spring Data auditing) |

`date` is optional — entries like adjustments or opening balances may not have a specific date. Dateless records count in totals but are excluded from `monthlyTrends`.

---

## Role Permission Matrix

| Action | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| Login | ✅ | ✅ | ✅ |
| View dashboard summary | ✅ | ✅ | ✅ |
| List all records | — | ✅ | ✅ |
| Filter records | — | ✅ | ✅ |
| Paginated record listing | — | ✅ | ✅ |
| Create record | — | — | ✅ |
| Update record | — | — | ✅ |
| Soft delete record | — | — | ✅ |
| List all users | — | — | ✅ |
| Get user by ID | — | — | ✅ |
| Update user role/status | — | — | ✅ |

---

## Setup & Running

**Prerequisites:** Java 17+, MongoDB running on `localhost:27017` (or a MongoDB Atlas URI)

```bash
# 1. Navigate into the project directory
cd finance

# 2. Set MongoDB URI (defaults to localhost if not set)
export MONGO_URI_FINANCE=mongodb://localhost:27017/finance_db

# For MongoDB Atlas:
# export MONGO_URI_FINANCE=mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/finance_db

# 3. Run (Maven wrapper included — no local Maven install needed)
./mvnw spring-boot:run

# Server starts at: http://localhost:8080
# Swagger UI at:    http://localhost:8080/swagger-ui.html
```

**application.properties:**
```properties
spring.data.mongodb.uri=${MONGO_URI_FINANCE:mongodb://localhost:27017/finance_db}
spring.application.name=finance
springdoc.swagger-ui.path=/swagger-ui.html
```

---

## API Documentation

All authenticated endpoints require:
```
Authorization: Bearer <jwt-token>
```

### Auth

---

#### `POST /api/auth/login`

Login with email and password. Returns a JWT token.

**Access:** Public (no token required)

**Request body:**
```json
{
  "email": "arjun@zorvyn.com",
  "password": "pass123"
}
```

**Sample curl:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "arjun@zorvyn.com", "password": "pass123"}'
```

**Success response (200):**
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NjFlOGE...
```
Raw JWT string. Use it as `Authorization: Bearer <this-value>`.

**Error responses:**
```json
{ "error": "User not found" }           // 404
{ "error": "Invalid password" }         // 500 (mapped via GlobalExceptionHandler)
```

---

### Users

`POST /api/users` is public for bootstrapping the first admin.
All other user endpoints require an `ADMIN` token.

---

#### `POST /api/users`

Create a new user. First call bootstraps your admin account.

**Access:** Public

**Request body:**
```json
{
  "name": "Arjun Admin",
  "email": "arjun@zorvyn.com",
  "password": "pass123",
  "role": "ADMIN"
}
```

**Sample curl:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Arjun Admin", "email": "arjun@zorvyn.com", "password": "pass123", "role": "ADMIN"}'
```

**Success response (201):**
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
> Note: `password` is never returned in responses.

**Validation errors (400):**
```json
{
  "name": "Name is required",
  "email": "Provide a valid email address",
  "role": "Role is required (VIEWER, ANALYST, ADMIN)"
}
```

**Duplicate email (400):**
```json
{ "error": "A user with this email already exists" }
```

---

#### `GET /api/users`

List all users.

**Access:** ADMIN only

**Sample curl:**
```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <admin-token>"
```

**Success response (200):**
```json
[
  {
    "id": "661e8a3f2c4b1a0012abcdef",
    "name": "Arjun Admin",
    "email": "arjun@zorvyn.com",
    "role": "ADMIN",
    "active": true,
    "createdAt": "2025-01-15T10:30:00"
  },
  {
    "id": "661e8b4a3d5c2b1123ghijkl",
    "name": "Priya Analyst",
    "email": "priya@zorvyn.com",
    "role": "ANALYST",
    "active": true,
    "createdAt": "2025-01-16T09:00:00"
  }
]
```

---

#### `GET /api/users/{id}`

Get a single user by their MongoDB ID.

**Access:** ADMIN only

**Sample curl:**
```bash
curl -X GET http://localhost:8080/api/users/661e8a3f2c4b1a0012abcdef \
  -H "Authorization: Bearer <admin-token>"
```

**Success response (200):** Same shape as individual user object above.

**Not found (404):**
```json
{ "error": "User not found with id: 661e8a3f2c4b1a0012abcdef" }
```

---

#### `PATCH /api/users/{id}`

Partially update a user's role and/or active status. Only provided fields are applied.

**Access:** ADMIN only

**Request body (all fields optional):**
```json
{ "role": "ANALYST" }
```
```json
{ "active": false }
```
```json
{ "role": "ADMIN", "active": true }
```

**Sample curl — deactivate a user:**
```bash
curl -X PATCH http://localhost:8080/api/users/661e8b4a3d5c2b1123ghijkl \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"active": false}'
```

**Success response (200):** Updated user object.

---

### Financial Records

All record endpoints require an authenticated token.

---

#### `POST /api/records`

Create a new financial record.

**Access:** ADMIN only

**Request body:**
```json
{
  "amount": 75000,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-01-15",
  "notes": "January salary"
}
```

`date` and `notes` are optional. `type` must be `INCOME` or `EXPENSE`.

**Sample curl:**
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 75000, "type": "INCOME", "category": "Salary", "date": "2025-01-15", "notes": "January salary"}'
```

**Success response (201):**
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

**Validation errors (400):**
```json
{
  "amount": "Amount must be greater than zero",
  "type": "Type is required (INCOME or EXPENSE)",
  "category": "Category is required"
}
```

---

#### `GET /api/records`

List all non-deleted records.

**Access:** ANALYST, ADMIN

**Sample curl:**
```bash
curl -X GET http://localhost:8080/api/records \
  -H "Authorization: Bearer <analyst-or-admin-token>"
```

**Success response (200):**
```json
[
  {
    "id": "661f1a2b3c4d5e6f7a8b9c0d",
    "amount": 75000.0,
    "type": "INCOME",
    "category": "Salary",
    "date": "2025-01-15",
    "notes": "January salary",
    "userId": "661e8a3f2c4b1a0012abcdef"
  }
]
```

---

#### `PUT /api/records/{id}`

Full replace update of a record. All fields except `id` and `userId` are replaced.

**Access:** ADMIN only

**Request body:**
```json
{
  "amount": 4200,
  "type": "EXPENSE",
  "category": "Rent",
  "date": "2025-02-01",
  "notes": "February rent"
}
```

**Sample curl:**
```bash
curl -X PUT http://localhost:8080/api/records/661f1a2b3c4d5e6f7a8b9c0d \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 4200, "type": "EXPENSE", "category": "Rent", "date": "2025-02-01"}'
```

**Success response (200):** Updated record object.

---

#### `DELETE /api/records/{id}`

Soft delete a record. Sets `deleted = true`, does NOT remove from MongoDB.

**Access:** ADMIN only

**Sample curl:**
```bash
curl -X DELETE http://localhost:8080/api/records/661f1a2b3c4d5e6f7a8b9c0d \
  -H "Authorization: Bearer <admin-token>"
```

**Success response:** `204 No Content` (empty body)

---

#### `GET /api/records/filter`

Filter records by any combination of type, category, date range, or keyword search. All params optional.

**Access:** ANALYST, ADMIN

**Query parameters:**

| Param | Type | Example | Notes |
|---|---|---|---|
| `type` | INCOME \| EXPENSE | `?type=INCOME` | Filter by record type |
| `category` | string | `?category=Salary` | Exact match |
| `from` | date (yyyy-MM-dd) | `?from=2025-01-01` | Requires `to` |
| `to` | date (yyyy-MM-dd) | `?to=2025-01-31` | Requires `from` |
| `search` | string | `?search=sal` | Case-insensitive partial match on category + notes. Overrides other params when present. |

**Sample curl — income in January:**
```bash
curl -X GET "http://localhost:8080/api/records/filter?type=INCOME&from=2025-01-01&to=2025-01-31" \
  -H "Authorization: Bearer <token>"
```

**Sample curl — filter by category:**
```bash
curl -X GET "http://localhost:8080/api/records/filter?category=Rent" \
  -H "Authorization: Bearer <token>"
```

**Sample curl — all three filters combined:**
```bash
curl -X GET "http://localhost:8080/api/records/filter?type=EXPENSE&category=Rent&from=2025-02-01&to=2025-02-28" \
  -H "Authorization: Bearer <token>"
```

**Sample curl — keyword search:**
```bash
curl -X GET "http://localhost:8080/api/records/filter?search=sal" \
  -H "Authorization: Bearer <token>"
```

**Success response (200):** Array of matching records.

**Date validation error (400):**
```json
{ "error": "From date cannot be after To date" }
```

---

#### `GET /api/records/paginated`

Paginated record listing, sorted newest first by date.

**Access:** ANALYST, ADMIN

**Query parameters:**

| Param | Default | Example |
|---|---|---|
| `page` | `0` | `?page=1` |
| `size` | `10` | `?size=5` |

**Sample curl:**
```bash
curl -X GET "http://localhost:8080/api/records/paginated?page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

**Success response (200):**
```json
{
  "data": [
    {
      "id": "661f1a2b3c4d5e6f7a8b9c0d",
      "amount": 75000.0,
      "type": "INCOME",
      "category": "Salary",
      "date": "2025-03-01"
    }
  ],
  "page": 0,
  "size": 10,
  "total": 42
}
```

---

### Dashboard

---

#### `GET /api/dashboard/summary`

Full aggregated dashboard summary. Accessible by all authenticated roles.

**Access:** VIEWER, ANALYST, ADMIN

**Sample curl:**
```bash
curl -X GET http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer <any-valid-token>"
```

**Success response (200):**
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
    {
      "id": "661f1a2b3c4d5e6f7a8b9c0d",
      "type": "INCOME",
      "amount": 75000.0,
      "category": "Salary",
      "date": "2025-03-01"
    }
  ],
  "monthlyTrends": {
    "2025-01": 60000.0,
    "2025-02": 71500.0,
    "2025-03": -500.0
  }
}
```

`monthlyTrends` values are net (income - expenses) per month, sorted chronologically. Negative values mean expenses exceeded income that month.

---

### Error Responses

All errors return a consistent JSON envelope:
```json
{ "error": "descriptive message" }
```

Validation errors return a field-level map:
```json
{
  "amount": "Amount must be greater than zero",
  "category": "Category is required"
}
```

| Status | When |
|---|---|
| `400 Bad Request` | Validation failure, duplicate email, invalid date range |
| `401 Unauthorized` | Missing/invalid/expired token, inactive account |
| `403 Forbidden` | Role not permitted for this action |
| `404 Not Found` | User or record not found |
| `429 Too Many Requests` | More than 100 requests from the same IP |
| `500 Internal Server Error` | Unexpected error (fallback handler) |

---

## Unit Tests

19 tests across two service classes. Run entirely without a live MongoDB — Mockito mocks all repositories.

```bash
# Run all tests
./mvnw test

# Run specific test classes
./mvnw test -Dtest="UserServiceTest,FinancialRecordServiceTest"
```

### UserServiceTest (9 tests)

| Test | What it verifies |
|---|---|
| `resolveCaller_missingHeader_throwsUnauthorized` | No auth context → 401 |
| `resolveCaller_inactiveUser_throwsUnauthorized` | Inactive account → 401 |
| `resolveCaller_activeUser_returnsUser` | Valid context → returns correct user |
| `getAllUsers_callerIsViewer_throwsAccessDenied` | VIEWER on user list → 403 |
| `getAllUsers_callerIsAdmin_returnsList` | ADMIN can list users |
| `updateUser_adminCanChangeRole` | Admin changes VIEWER → ANALYST |
| `updateUser_adminCanDeactivateUser` | Admin sets `active = false` |
| `createUser_duplicateEmail_throwsIllegalArgument` | Duplicate email → 400 |
| `createUser_newEmail_savesAndReturns` | Valid creation saves and returns user |

### FinancialRecordServiceTest (10 tests)

| Test | What it verifies |
|---|---|
| `createRecord_viewerCannotCreate_throwsAccessDenied` | VIEWER cannot create → 403 |
| `createRecord_analystCannotCreate_throwsAccessDenied` | ANALYST cannot create → 403 |
| `createRecord_adminCanCreate_savesRecord` | ADMIN creates, `userId` set on record |
| `getAllRecords_viewerCannotAccess_throwsAccessDenied` | VIEWER cannot list → 403 |
| `getAllRecords_analystCanRead_returnsList` | ANALYST can list records |
| `deleteRecord_recordNotFound_throwsNotFound` | Unknown ID → 404 |
| `deleteRecord_adminWithValidId_setsDeletedTrue` | Soft delete sets `deleted=true`, never calls `deleteById` |
| `filterRecords_typeAndDateRange_usesCombinedQuery` | type+dateRange routes to correct repository method |
| `filterRecords_keywordSearch_usesCategoryKeyword` | `search` param uses keyword repository method |
| `filterRecords_noParams_returnsAll` | No params → `findByDeletedFalse()` |
| `filterRecords_viewer_throwsAccessDenied` | VIEWER on filter → 403 |
| `getPaginated_viewer_throwsAccessDenied` | VIEWER on paginated → 403 |

---

## Optional Enhancements Implemented

### JWT Authentication
Full token lifecycle: BCrypt password hashing at registration, HMAC-SHA256 signed JWT at login, token validation on every request via `AuthFilter`, role extracted from token claims and injected into Spring `SecurityContext`.

### Pagination
`GET /api/records/paginated?page=0&size=10` returns a `PageResponse<FinancialRecord>` with `data`, `page`, `size`, and `total` count. Sorted newest-first by date using Spring Data's `PageRequest` with `Sort.Direction.DESC`.

### Keyword Search
`GET /api/records/filter?search=sal` runs a case-insensitive partial match against both `category` and `notes` fields using MongoDB's regex behavior via Spring Data method naming.

### Soft Delete
Records are never removed from MongoDB. `DELETE /api/records/{id}` sets `deleted = true`. All query methods include `AndDeletedFalse` to automatically exclude them. Historical data is preserved for audit purposes.

### Rate Limiting
`AuthFilter` maintains a `ConcurrentHashMap<IP, count>`. Any IP exceeding 100 requests receives `HTTP 429` immediately, before authentication or routing. This is in-memory and resets on server restart.

### Unit Tests
19 tests covering all service-layer access control paths, edge cases (inactive users, not-found records, soft delete verification), and filter routing logic. No live database required — all repositories are mocked with Mockito.

### Swagger / OpenAPI
SpringDoc OpenAPI integrated via `springdoc-openapi-starter-webmvc-ui`. Available at `http://localhost:8080/swagger-ui.html` when the server is running. Provides interactive API explorer where you can test all endpoints directly from the browser.

---

## Assumptions & Tradeoffs

**JWT secret is hardcoded for assessment** — The HMAC key lives in `JwtUtil.java`. In production this would be read from an environment variable or secrets manager. Tokens have no expiry set — production would use `.setExpiration(new Date(System.currentTimeMillis() + 86400000))`.

**Role checks at two layers** — `@PreAuthorize` on controllers + explicit `assertAdmin()`/`assertNotViewer()` in services. The redundancy is intentional: the service layer ensures enforcement even if the controller annotation is misconfigured or the service is called from another internal service.

**Soft delete as default** — Records are never permanently removed. Converting to hard delete later requires removing the `deleted` flag from the entity and renaming the repository methods.

**8 explicit filter methods instead of dynamic queries** — Every param combination is an explicit, named repository method. This makes it impossible for one filter to silently override another. The tradeoff is verbosity; the benefit is complete predictability.

**MongoDB aggregation for dashboard** — Upgraded from Java stream aggregation to MongoDB's aggregation pipeline. This means totals and trends are computed in the database, not in application memory, which scales much better for large datasets.

**Unique email enforced at two levels** — MongoDB unique index prevents duplicates at the DB level; `existsByEmail()` in the service returns a clean `400` response rather than a raw `DuplicateKeyException` from MongoDB.

**In-memory rate limiting** — Per-IP counter in `AuthFilter` using `ConcurrentHashMap`. Resets on restart. Production would use Redis for persistent, distributed rate limiting.

**`POST /api/users` is public** — Necessary to bootstrap the first admin account. All subsequent user management (listing, updating) requires an admin JWT.

**`date` optional on records** — Covers entries like adjustments or opening balances with no specific date. These records count in income/expense totals but are excluded from `monthlyTrends` since there's no date to group by.
