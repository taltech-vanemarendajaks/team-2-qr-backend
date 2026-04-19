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
- **Google OAuth**: `GoogleAuthService` validates the ID token **locally** using `GoogleIdTokenVerifier` (no remote HTTP call); `LoginService.googleLogin` looks up or auto-creates the user. Username is derived as a display name (first word of name, or email prefix) — no uniqueness constraint.
- **Image storage**: Receipt images stored as `bytea` in the DB. `BytesConverter` (`infrastructure/util/`) handles Base64 ↔ `byte[]`.
- **Password reset tokens**: `PasswordResetService` generates UUID tokens stored in `mystuff.password_reset_token`. Tokens expire after a configurable TTL (default 60 min) and are single-use (`used` flag). `EmailService` delivers the reset link via Mailtrap. The forgot-password endpoint always returns 200 to avoid leaking whether an email is registered. Unknown-email requests sleep 200–500 ms to prevent timing-based enumeration.
- **Timing-safe password comparison**: `LoginService` uses `MessageDigest.isEqual` when comparing legacy plaintext passwords to prevent timing oracle attacks.
- **User defaults**: `User` entity initialises `createdAt = OffsetDateTime.now()` and `authProvider = "PASSWORD"` at construction time.
- **Session establishment**: `LoginController.establishSession()` populates Spring Security's `GrantedAuthority` list from the user's role so `@PreAuthorize` role checks work. `GET /api/auth/me` returns **204 No Content** when there is no active session.

### Database

PostgreSQL 16, schema `mystuff`. DDL is schema-first (`ddl-auto=none`); schema lives in `database/2_create.sql`, seed data in `database/3_import.sql`. P6Spy wraps the JDBC driver for SQL logging.

Default credentials (local/Docker): loaded from `.env` (`DB_USERNAME` / `DB_PASSWORD`), database `postgres`.

### Security

- Spring Security filter chain in `SecurityConfig`: sessions enabled (`IF_REQUIRED`), CSRF disabled.
- **COOP header**: `SecurityConfig` sets `Cross-Origin-Opener-Policy: same-origin-allow-popups` to support Google OAuth popup flows.
- Public routes: `/api/auth/login`, `/api/auth/google`, `/api/auth/logout`, `/api/auth/signup`, `/api/auth/forgot-password`, `/api/auth/reset-password`, Swagger UI. All other routes require an authenticated session.
- Auth is session-based — `LoginController` stores the logged-in `User` in `HttpSession` after credential or Google token verification.
- BCrypt password hashing via `PasswordConfig` bean.
- CORS configured in `CorsConfig`.
- **Session cookie hardening**: `application.properties` sets `SameSite=Strict`, `HttpOnly=true`, `Secure=true` on the session cookie.
- **Request size limits**: Tomcat and multipart uploads are capped at 15 MB to mitigate DoS via large payloads. Set above the 10 MB image cap to accommodate Base64 encoding overhead (~33%).

### Exception handling

Two `@ControllerAdvice` classes work together:
- `RestExceptionHandler` (extends `ResponseEntityExceptionHandler`): handles `ForbiddenException` (403), `DataNotFoundException` / `PrimaryKeyNotFoundException` (404), `BadRequestException` (400), and `MethodArgumentNotValidException` (400). Collects **all** field errors, joined by `"; "`.
- `GlobalExceptionHandler`: handles `ConstraintViolationException` (400, all violations collected), `TooManyRequestsException` (429), and catch-all `Exception` (500).

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
- `PasswordResetIntegrationTest` — forgot-password (valid email, unknown email, invalid format), reset-password (valid token, expired, used, non-existent), token single-use enforcement, validation edge cases; mocks `MailtrapClient`

### Configuration

Key `application.properties` values:
- `mystuff.item.path=/item?itemId=` — base URL fragment for QR codes
- `mystuff.server.address` — set to frontend origin for QR URL construction
- `mystuff.reset.token-ttl-minutes` — password reset token TTL (default 60)
- `mystuff.reset.link-base` — base URL for the reset link sent in emails
- `mailtrap.api-token` / `mailtrap.from-email` / `mailtrap.from-name` — Mailtrap email config
- Secrets loaded from `.env`: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `DB_USERNAME`, `DB_PASSWORD`, `MAILTRAP_API_TOKEN`, `MAILTRAP_FROM_EMAIL`, `MAILTRAP_FROM_NAME`, `RESET_LINK_BASE`
