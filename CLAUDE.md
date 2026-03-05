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

# Run with Docker (recommended — starts PostgreSQL + backend)
docker compose up -d
docker logs -f team2-backend

# Reset database (drops volume, re-initializes from database/*.sql)
docker compose down -v && docker compose up -d
```

API is available at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Architecture

**Spring Boot 3.5.4 / Java 21** REST backend for a personal inventory app where users tag belongings with QR codes.

### Layer structure

```
ee.valiit.mystuffback/
├── controller/      HTTP endpoints + request/response DTOs
├── service/         Business logic
├── persistence/     JPA entities + repositories + MapStruct mappers
└── infrastructure/  Config, global exception handler, custom exceptions, utilities
```

The controller layer is kept thin; all business logic lives in services. DTOs are defined alongside their controller. Entities are in `persistence/<domain>/`.

### Key architectural patterns

- **MapStruct** (`@Mapper`) handles entity ↔ DTO conversion — look in `persistence/<domain>/` for mappers.
- **Soft deletes**: Items use `status` varchar ('A' = active, 'D' = deleted). Queries filter on `status = 'A'`.
- **In-memory rate limiting**: `RateLimitService` uses a sliding window keyed by `endpoint:clientIp`. Applied via `@PostMapping` handlers in `LoginController`.
- **Support token flow**: Two-step verification in `SupportService` — verify QR ownership → get 10-min single-use token → submit support request.
- **BCrypt migration**: `LoginService` auto-migrates legacy plaintext passwords to BCrypt on first login.
- **Image storage**: Receipt images stored as `bytea` in the DB. `BytesConverter` (`infrastructure/util/`) handles Base64 ↔ `byte[]`.

### Database

PostgreSQL 16, schema `mystuff`. DDL is schema-first (`ddl-auto=none`); schema lives in `database/2_create.sql`, seed data in `database/3_import.sql`. P6Spy wraps the JDBC driver for SQL logging.

Default credentials (local/Docker): `postgres` / `student123`, database `postgres`.

### Security

- No Spring Security filter chain — auth is manual (BCrypt via `PasswordConfig` bean, no JWT/sessions).
- Honeypot field (`website`) checked in login and support endpoints; filled → 403.
- CORS allows `http://localhost:8081` and `http://localhost:8082`.

### Configuration

Key `application.properties` values:
- `qr.code.path=/item?itemId=` — base URL fragment for QR codes
- `captcha.secret` — hCaptcha secret for signup
- `server.address` — set to frontend origin for QR URL construction
