# E-Commerce Mini Backend

A side project for Java Backend Developer.
It demonstrates five core criteria in a single, runnable codebase:
**Pagination**, **Transaction**, **Spring Security + JWT**, **Global Exception Handling**, and **Unit Testing** (JUnit 5 + Mockito).

The project supports two run modes: **local with H2** (no setup required, ideal for development) and **Docker + PostgreSQL** (production-like, data persists across restarts).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| Persistence | Spring Data JPA / Hibernate |
| Database (local) | H2 in-memory |
| Database (Docker) | PostgreSQL 16 |
| Build | Maven 3.6+ |
| Testing | JUnit 5 + Mockito |
| Containerization | Docker + Docker Compose |

---

## Key Implementation Highlights

| Criterion | What was implemented |
|---|---|
| **1. Pagination** | `GET /api/products` and `GET /api/orders/my` return a custom `PageResponse<T>` wrapper with `page`, `pageSize`, `totalElements`, `totalPages`, and `content`. Client controls page/size/sort via query params. Max page size capped at 50 via `spring.data.web.pageable.max-page-size` to prevent DB exhaustion. |
| **2. Transaction** | `OrderService.createOrder` runs inside a single `@Transactional` — stock is decremented and the order is saved atomically. Any failure (e.g. insufficient stock) triggers a full rollback. `@Transactional(readOnly = true)` is used for all query-only methods. |
| **3. Security / JWT** | Stateless JWT authentication via `JwtAuthFilter`. Role-based access control (`ROLE_USER`, `ROLE_ADMIN`) enforced with `@PreAuthorize`. IDOR protection: `GET /api/orders/{id}` verifies the requesting user owns the order (or is ADMIN) before returning data — role check alone is not sufficient. |
| **4. Exception Handling** | `GlobalExceptionHandler` catches `ResourceNotFoundException` (404), `InsufficientStockException` (400), `AccessDeniedException` (403), validation errors (400), and all uncaught exceptions (500). Every error response follows a consistent `ErrorResponse` JSON structure — no raw stack traces exposed to clients. |
| **5. Unit Test** | `OrderServiceTest` and `ProductServiceTest` cover happy-path and edge cases (insufficient stock, product not found, order not found) using Mockito mocks — no Spring context or real DB needed. |

**Bonus design decisions worth knowing:**
- **Optimistic Locking** (`@Version` on `Product`) prevents oversell when two requests attempt to purchase the last item concurrently — the second transaction gets an `ObjectOptimisticLockingFailureException` and is rejected.
- **Price snapshot** — `OrderItem` stores `price` at the time of purchase, not a foreign key to the current product price. This preserves order history correctly even if prices change later.
- **DTO layer** — all controllers return dedicated response DTOs (`ProductResponse`, `OrderResponse`, `CategoryResponse`, etc.) rather than raw JPA entities, avoiding lazy-loading exceptions during JSON serialization.

---

## Getting Started

### Option 1 — Local with H2 (no Docker, no database setup)

**Requirements:** JDK 17+, Maven 3.6+

```bash
# Build + run all unit tests + start the app
mvn clean install
mvn spring-boot:run
```

App runs at `http://localhost:8080`.

```bash
# Build only, skip tests (faster iteration)
mvn clean install -DskipTests

# Run tests only
mvn test
```

While the app is running, inspect the in-memory database at `http://localhost:8080/h2-console`:
- **JDBC URL:** `jdbc:h2:mem:ecommerce_db`
- **Username:** `sa` | **Password:** *(leave empty)*

> Data is lost on every restart — `DataInitializer` re-seeds it automatically on startup.

---

### Option 2 — Docker + PostgreSQL (recommended for demo / running on another machine)

**Requirements:** Docker Desktop (includes Docker Compose)

```bash
# 1. Copy the env template and fill in your own values
cp .env.example .env
#    Edit .env: set POSTGRES_PASSWORD and JWT_SECRET to something non-trivial

# 2. Build the image and start both containers
docker compose up -d

# 3. Follow app logs
docker compose logs -f app
```

App runs at `http://localhost:8080`. PostgreSQL is available at `localhost:5432`.

```bash
# Stop containers but keep PostgreSQL data (volume survives)
docker compose down

# Stop containers AND delete all data (full reset)
docker compose down -v

# Rebuild after code changes
docker compose build app && docker compose up -d
```

**Why does data survive shutdown?**
PostgreSQL data is stored in the Docker named volume `postgres_data` (defined in `docker-compose.yml`). This volume exists independently of the container — it survives `docker compose down`, machine restarts, and Docker Desktop restarts. Only `docker compose down -v` removes it.

> `.env` contains real secrets (password, JWT key) and is listed in `.gitignore` — never commit it. Use `.env.example` as the template when cloning to a new machine.

---

## Pre-seeded Data

`DataInitializer` runs on every startup and creates the following data if the tables are empty.

### Accounts

| Username | Password | Role  |
|----------|----------|-------|
| admin    | admin123 | ADMIN |
| manager  | admin123 | ADMIN |
| user1    | user123  | USER  |
| user2    | user123  | USER  |
| user3    | user123  | USER  |
| user4    | user123  | USER  |
| user5    | user123  | USER  |

### Other seed data

| Type | Count | Notes |
|---|---|---|
| Categories | 6 | Electronics, Books, Fashion, Home & Kitchen, Sports, Toys |
| Products | 25 | Spread across all 6 categories; one product has `stockQuantity = 0` to test out-of-stock validation |
| Orders | 10 | Belong to user1–user5 with mixed statuses: PENDING / CONFIRMED / CANCELLED |

---

## API Quick Reference

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | None | Register a new USER account |
| POST | `/api/auth/login` | None | Login — returns a JWT token |

### Products

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/products` | None | List products (paginated, sortable) |
| GET | `/api/products/{id}` | None | Get a single product |
| POST | `/api/products` | ADMIN | Create a product |

Query params for listing: `page` (0-indexed), `size` (max 50), `sort` (e.g. `price,desc`)

### Categories

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/categories` | None | List all categories |
| POST | `/api/categories` | ADMIN | Create a category |

### Orders

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/orders` | USER / ADMIN | Place an order (transactional, decrements stock) |
| GET | `/api/orders/my` | USER / ADMIN | List own orders (paginated) |
| GET | `/api/orders/{id}` | USER / ADMIN | Get order by id — USER can only view their own orders |

---

## End-to-End Test Flow (curl)

### Step 1 — Login as ADMIN, capture the token

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

ADMIN_TOKEN="<paste token here>"
```

### Step 2 — ADMIN creates a product (criterion 3: role-based access)

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"name":"Laptop Dell XPS 13","price":25000000,"stockQuantity":5,"categoryId":1}'
```

### Step 3 — List products with pagination (criterion 1)

```bash
# page 0, 5 items per page, sorted by price descending
curl "http://localhost:8080/api/products?page=0&size=5&sort=price,desc"
```

### Step 4 — Login as USER

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"user123"}'

USER_TOKEN="<paste token here>"
```

### Step 5 — USER tries to create a product → must get 403 (criterion 3 + 4)

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"name":"Hack","price":1,"stockQuantity":1,"categoryId":1}'
# Expected: HTTP 403, consistent JSON error body — no stack trace
```

### Step 6 — USER places an order (criterion 2: @Transactional)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"items":[{"productId":1,"quantity":2}]}'
```

### Step 7 — Try to exceed stock → must fail and rollback

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"items":[{"productId":1,"quantity":9999}]}'
# Expected: HTTP 400, stock of product 1 unchanged (rollback verified)
```

### Step 8 — List own orders

```bash
curl "http://localhost:8080/api/orders/my" \
  -H "Authorization: Bearer $USER_TOKEN"
```

### Step 9 — Get order by id — IDOR protection check (criterion 3)

```bash
# View an order that belongs to user1 — should succeed
curl -i "http://localhost:8080/api/orders/1" \
  -H "Authorization: Bearer $USER_TOKEN"

# View an order that belongs to a different user — must get 403
# (find an order id from another user in the seed data, e.g. id=6 belongs to user3)
curl -i "http://localhost:8080/api/orders/6" \
  -H "Authorization: Bearer $USER_TOKEN"
# Expected: HTTP 403 Forbidden — data of another user is never exposed

# ADMIN can view any order
curl -i "http://localhost:8080/api/orders/6" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# Expected: HTTP 200
```

### Step 10 — Fetch a non-existent resource → check error format (criterion 4)

```bash
curl -i "http://localhost:8080/api/products/9999"
# Expected: HTTP 404, JSON with message field — no stack trace
```

---

## Criteria → Code Map

| Criterion | Primary files |
|---|---|
| 1. Pagination | `ProductController.getAllProducts`, `ProductService.getAllProducts`, `PageResponse.java` |
| 2. @Transactional | `OrderService.createOrder` |
| 3. Security / JWT | `SecurityConfig`, `JwtUtil`, `JwtAuthFilter`, `CustomUserDetailsService` |
| 3. IDOR protection | `OrderController.getOrderById`, `OrderService.getOrderById` |
| 4. Exception Handling | `GlobalExceptionHandler`, `exception/` package |
| 5. Unit Test | `OrderServiceTest`, `ProductServiceTest` |
| Optimistic Locking | `Product.java` (`@Version` field) |
| Docker / PostgreSQL | `Dockerfile`, `docker-compose.yml`, `application-docker.yml` |

---

## Common Interview Q&A

**Why does `OrderItem` store `price` instead of referencing `Product.price`?**
Prices change over time. Storing the price at the moment of purchase (snapshot) ensures order history is always accurate, regardless of future price updates.

**When does `@Transactional` roll back?**
By default, Spring rolls back on unchecked exceptions (`RuntimeException` and its subclasses). Checked exceptions do **not** trigger a rollback unless you explicitly configure `@Transactional(rollbackFor = ...)`. All custom exceptions in this project extend `RuntimeException` for this reason.

**What is IDOR and how is it prevented here?**
IDOR (Insecure Direct Object Reference) is when an API exposes internal object IDs and a user can access another user's data just by guessing the ID. `@PreAuthorize("hasAnyRole('USER','ADMIN')")` only checks *who can call the endpoint* — it cannot know *which order belongs to whom*. The fix is in `OrderService.getOrderById`: after loading the order from the DB, it compares `order.getUser().getUsername()` with the authenticated username from the JWT. If they don't match and the caller is not an ADMIN, an `AccessDeniedException` (→ HTTP 403) is thrown.

**How does Optimistic Locking prevent oversell?**
`Product` has a `@Version Long version` field. When two transactions read the same product and both try to update `stockQuantity`, Hibernate checks the version on write. The second commit finds the version incremented by the first and throws `ObjectOptimisticLockingFailureException` — the second purchase fails cleanly instead of silently corrupting stock data.

**Why set `max-page-size` in `application.yml` instead of `@PageableDefault`?**
`@PageableDefault` only sets the *fallback* value when the client sends no `size` param — it does not cap what the client *can* request. Without an explicit maximum, a client can call `?size=999999` and force a full table scan in one request (DoS risk). `spring.data.web.pageable.max-page-size: 50` is enforced by Spring's `PageableHandlerMethodArgumentResolver` and applies globally to every pageable endpoint without touching controller code.

**Why doesn't PostgreSQL data disappear when the machine shuts down?**
The `docker-compose.yml` mounts a Docker *named volume* (`postgres_data`) onto `/var/lib/postgresql/data`. Named volumes are managed by Docker and survive container stops, `docker compose down`, and machine reboots. Data is only removed when you explicitly run `docker compose down -v`.
