# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run locally (requires PostgreSQL on localhost:5432)
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "ee.valiit.mystuffback.MystuffbackApplicationTests"

# Run with Docker (recommended — starts PostgreSQL + backend)
docker compose up -d
docker logs -f team2-backend

# Reset database (drops volume, re-initializes from database/*.sql)
docker compose down -v && docker compose up -d
```

API is available at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Architecture

**Spring Boot 4 / Java 25** REST backend for a personal inventory app where users tag belongings with QR codes.

### Layer structure

```
ee.valiit.mystuffback/
├── controller/      HTTP endpoints + request/response DTOs
├── service/         Business logic
├── persistence/     JPA entities + repositories + MapStruct mappers
└── infrastructure/  Config, exception handlers, custom exceptions, utilities
```

The controller layer is kept thin; all business logic lives in services. DTOs are defined alongside their controller. Entities are in `persistence/<domain>/`.

### Key architectural patterns

- **MapStruct** (`@Mapper`) handles entity ↔ DTO conversion — look in `persistence/<domain>/` for mappers.
- **Soft deletes**: Items use `status` varchar ('A' = active, 'D' = deleted). Queries filter on `status = 'A'`.
- **In-memory rate limiting**: `RateLimitService` uses a sliding window keyed by `endpoint:clientIp`. Applied via `@PostMapping` handlers in `LoginController`.
- **BCrypt migration**: `LoginService` auto-migrates legacy plaintext passwords to BCrypt on first login.
- **Google OAuth**: `GoogleAuthService` validates the ID token; `LoginService.googleLogin` looks up or auto-creates the user. Username is derived as a display name (first word of name, or email prefix) — no uniqueness constraint.
- **Image storage**: Receipt images stored as `bytea` in the DB. `BytesConverter` (`infrastructure/util/`) handles Base64 ↔ `byte[]`.

### Database

PostgreSQL 16, schema `mystuff`. DDL is schema-first (`ddl-auto=none`); schema lives in `database/2_create.sql`, seed data in `database/3_import.sql`. P6Spy wraps the JDBC driver for SQL logging.

Default credentials (local/Docker): loaded from `.env` (`DB_USERNAME` / `DB_PASSWORD`), database `postgres`.

### Security

- Spring Security filter chain in `SecurityConfig`: sessions enabled (`IF_REQUIRED`), CSRF disabled.
- Public routes: `/api/auth/login`, `/api/auth/google`, `/api/auth/logout`, `/api/auth/signup`, Swagger UI. All other routes require an authenticated session.
- Auth is session-based — `LoginController` stores the logged-in `User` in `HttpSession` after credential or Google token verification.
- BCrypt password hashing via `PasswordConfig` bean.
- CORS configured in `CorsConfig`.

### Exception handling

Two `@ControllerAdvice` classes work together:
- `RestExceptionHandler` (extends `ResponseEntityExceptionHandler`): handles `ForbiddenException` (403), `DataNotFoundException` / `PrimaryKeyNotFoundException` (404), and `MethodArgumentNotValidException` (400).
- `GlobalExceptionHandler`: handles `ConstraintViolationException` (400), `TooManyRequestsException` (429), and catch-all `Exception` (500).

### Testing

Integration tests use **Testcontainers** (a real PostgreSQL 16 container) via `AbstractIntegrationTest`. The container starts once and is shared across all test classes via Spring's context cache.

- `src/test/resources/db/schema.sql` — mirrors `database/2_create.sql` for the test container
- `src/test/resources/db/roles.sql` — seeds roles once at startup
- `src/test/resources/cleanup.sql` — run after each test method via `@Sql` to hard-delete test data
- `src/test/resources/application.properties` — stubs out Google OAuth and P6Spy; datasource URL is overridden by `@DynamicPropertySource`

Test classes:
- `ItemControllerTest` — ownership enforcement (IDOR), session scoping, unauthenticated access
- `UserRegistrationTest` — duplicate email rejected, duplicate username allowed
- `LoginServiceTest` — `deriveDisplayName` logic for Google login (unit test with Mockito)

### Configuration

Key `application.properties` values:
- `mystuff.item.path=/item?itemId=` — base URL fragment for QR codes
- `mystuff.server.address` — set to frontend origin for QR URL construction
- Secrets loaded from `.env`: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `DB_USERNAME`, `DB_PASSWORD`
