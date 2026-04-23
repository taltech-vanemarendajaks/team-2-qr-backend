# MyStuffLabelled – Project Documentation

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Database Schema](#3-database-schema)
4. [API Reference](#4-api-reference)
5. [Security](#5-security)
6. [Development Setup](#6-development-setup)
7. [Roadmap & Team Tasks](#7-roadmap--team-tasks)
8. [Development Guidelines](#8-development-guidelines)

## 1. Project Overview

**MyStuffLabelled** is a personal inventory management system. Users register their belongings — appliances, electronics, tools — with metadata like purchase date, model number, warranty notes, and receipt photos. Each item gets a unique QR code. Scanning the QR code shows the item's details.

### Key Capabilities
- Register and manage personal items with metadata and photos
- Generate and scan QR codes to access item details
- Contact admin via a secure support request form (QR token verified)
- Role-based access: `customer` and `admin`

### Repositories
| Component | Repository |
|-----------|------------|
| Backend | https://github.com/taltech-vanemarendajaks/team-2-qr-backend |
| Frontend | https://github.com/taltech-vanemarendajaks/team-2-qr-front |

### Tech Stack

**Backend**
| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Build tool | Gradle 9.2.1 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| DTO mapping | MapStruct 1.6.3 |
| API docs | SpringDoc OpenAPI 2.8.14 (Swagger UI) |
| Password hashing | Spring Security Crypto (BCrypt) |
| Containerisation | Docker / Docker Compose |

**Frontend**
| Layer | Technology |
|-------|------------|
| Framework | Vue.js 3.2.13 |
| Build tool | Vue CLI 5.0.0 |
| Language | JavaScript (ES6+) |
| Routing | Vue Router 4 |
| HTTP client | Axios 1.7.9 |
| UI framework | Bootstrap 5.3.3 |
| Icons | Font Awesome 6.7.2 |
| QR code rendering | qrcode.vue 3.6.0 |
| Auth | Google Sign-In SDK (OAuth 2.0) |

## 2. Architecture

### System Overview

```
Browser (Vue.js SPA)
  └── HTTP / Axios (localhost:8081 dev → proxied to localhost:8080)
        └── Controller (REST, input validation)
              └── Service (business logic)
                    └── Repository (JPA, PostgreSQL)
```

The frontend is a single-page application served separately from the backend. During development, Vue CLI's dev server runs on port **8081** and proxies all API requests to the backend on port **8080**.

## 3. Database Schema

Schema name: `mystuff`
Files: [database/2_create.sql](database/2_create.sql), [database/3_import.sql](database/3_import.sql)

### Entity-Relationship Overview

```
role ──< user ──< item ──< image
              └──< password_reset_token
```

| Relationship | Type | Description |
|---|---|---|
| role → user | 1:N | A role has many users |
| user → item | 1:N | A user owns many items |
| item → image | 1:N | An item has many images |
| user → password_reset_token | 1:N | A user can have many reset tokens |

### Tables

#### `mystuff.role`
| Column | Constraints | Notes |
|--------|-------------|-------|
| id | PK | |
| name | NOT NULL | `'admin'` or `'customer'` |

#### `mystuff.user`
| Column | Constraints | Notes |
|--------|-------------|-------|
| id | PK | |
| role_id | FK → role.id | |
| username | NOT NULL | |
| password | NOT NULL | BCrypt hash |
| email | NOT NULL | |
| status | NOT NULL | `'A'` = active |

#### `mystuff.item`
| Column | Constraints | Notes |
|--------|-------------|-------|
| id | PK | |
| user_id | FK → user.id, NOT NULL | |
| name | NOT NULL | |
| date | NOT NULL | Purchase/acquisition date |
| model | NULL | Optional model number |
| comment | NULL | Notes, warranty info, etc. |
| status | NOT NULL | `'A'` = active, `'D'` = deleted |
| qr_token | NULL, UNIQUE | UUID assigned when QR is generated |

#### `mystuff.image`
| Column | Constraints | Notes |
|--------|-------------|-------|
| id | PK | |
| item_id | FK → item.id, NOT NULL | |
| image_data | NOT NULL | Receipt or item photo (binary) |

#### `mystuff.password_reset_token`
| Column | Constraints | Notes |
|--------|-------------|-------|
| id | PK | serial |
| user_id | FK → user.id, NOT NULL | |
| token | varchar(36), NOT NULL, UNIQUE | UUID |
| expires_at | timestamptz, NOT NULL | |
| used | boolean, NOT NULL, DEFAULT false | Single-use flag |
| created_at | timestamptz, NOT NULL, DEFAULT now() | |

### Soft Delete
Items are never hard-deleted. Setting `status = 'D'` hides the item from all queries.

## 4. API Reference

Interactive docs available at: `http://localhost:8080/swagger-ui.html`

### Authentication

#### `POST /api/auth/signup`
Register a new user account.

**Rate limit:** 5 requests / 60 seconds per IP

**Request body:**
```json
{
  "username": "string",
  "password": "string",
  "email": "string"
}
```

**Responses:**
| Status | Meaning |
|--------|---------|
| 201 | Account created |
| 400 | Validation error |
| 409 | Username already taken (error code 222) |
| 429 | Too many requests |

#### `POST /api/auth/login`
Authenticate and receive user identity.

**Rate limit:** 10 requests / 60 seconds per IP

**Request body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Response (200):**
```json
{
  "userId": 1,
  "roleName": "customer",
  "username": "string"
}
```

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Login successful |
| 403 | Wrong credentials (error code 111) |
| 429 | Too many requests |

#### `POST /api/auth/google`
Authenticate via Google OAuth. Receives a Google ID token and returns user identity.

**Request body:**
```json
{
  "idToken": "string"
}
```

**Response (200):**
```json
{
  "userId": 1,
  "roleName": "customer",
  "username": "string"
}
```

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Login successful |
| 403 | Invalid Google token |

#### `POST /api/auth/forgot-password`
Request a password reset email. Always returns 200 to prevent email enumeration.

**Rate limit:** 3 requests / 5 minutes per IP

**Request body:**
```json
{
  "email": "user@example.com"
}
```

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Request received (regardless of whether the email is registered) |
| 400 | Invalid email format |
| 429 | Too many requests |

#### `POST /api/auth/reset-password`
Complete a password reset using a token from the reset email.

**Rate limit:** 10 requests / 60 seconds per IP

**Request body:**
```json
{
  "token": "uuid-string",
  "newPassword": "string (8–100 characters)"
}
```

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Password reset successfully |
| 400 | Validation error or invalid/expired/used token (error code 551) |
| 429 | Too many requests |

#### `POST /api/auth/change-password`
Change the password for the currently logged-in user.

**Requires:** authenticated session

**Rate limit:** 5 requests / 60 seconds per user

**Request body:**
```json
{
  "currentPassword": "string",
  "newPassword": "string (8–100 characters)"
}
```

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Password changed successfully |
| 400 | Current password is incorrect (error code 552) or validation error |
| 429 | Too many requests |

### Items

#### `POST /api/item`
Create a new item.

**Query params:** `userId` (required)

**Request body:**
```json
{
  "itemName": "string (max 50)",
  "date": "2024-01-15",
  "model": "string (max 250, optional)",
  "comment": "string (max 500, optional)",
  "imageData": "base64-encoded-string (optional)"
}
```

**Responses:**
| Status | Meaning |
|--------|---------|
| 201 | Item created |
| 409 | Item name already exists for this user (error code 333) |

#### `GET /api/item/all`
Get all active items for a user.

**Query params:** `userId` (required)

#### `GET /api/item`
Get full details of a single item, including images.

**Query params:** `itemId` (required)

#### `PUT /api/item`
Update an item's details.

**Query params:** `itemId` (required)

**Request body:** Same fields as POST `/api/item`

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Updated |
| 404 | Item not found |

#### `DELETE /api/item`
Soft-delete an item (sets `status = 'D'`).

**Query params:** `itemId` (required)

#### `DELETE /api/item/{itemId}/images/{imageId}`
Remove a specific image from an item.

**Path params:** `itemId`, `imageId`

### QR Codes

#### `GET /api/qr-code`
Get the QR code URL for an item. Assigns a `qr_token` if one doesn't exist yet.

**Query params:** `itemId` (required)

**Response (200):**
```json
{
  "qrUrl": "http://localhost:8081/item?itemId=1&t=550e8400-e29b-41d4-a716-446655440000"
}
```

### Support

#### `POST /api/support/verify-qr`
Verify QR ownership and receive a short-lived support token.

**Request body:**
```json
{
  "username": "string",
  "email": "user@example.com",
  "qrToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (200):**
```json
{
  "supportToken": "string",
  "email": "user@example.com"
}
```

#### `POST /api/support/request`
Submit a support request. Requires a valid token from `/verify-qr`.

**Request body:**
```json
{
  "supportToken": "string",
  "message": "string"
}
```

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Request submitted |
| 403 | Invalid or expired support token |

> Support tokens are valid for **10 minutes** and are **single-use**.

### Error Codes

| Error Code | Meaning |
|------------|---------|
| 111 | Incorrect username or password |
| 222 | Username already taken |
| 333 | Item name already exists for this user |
| 551 | Invalid or expired password reset token |
| 552 | Current password is incorrect |

## 5. Security

### Password Hashing
All passwords are stored as BCrypt hashes. Legacy plaintext passwords are automatically re-hashed on first login. Legacy plaintext comparison uses `MessageDigest.isEqual` (constant-time) to prevent timing oracle attacks.

### Google Token Verification
`GoogleAuthService` verifies Google ID tokens **locally** using `GoogleIdTokenVerifier` (`com.google.api-client:google-api-client:2.7.0`). No token data is sent to a remote endpoint.

### Session Cookie Hardening
The session cookie is configured with `SameSite=Strict` and `HttpOnly=true`. The `Secure` flag is controlled by the `SESSION_COOKIE_SECURE` environment variable (defaults to `false`). Set it to `true` only when the server runs behind HTTPS — leaving it `false` on plain HTTP prevents the browser from silently dropping the session cookie.

### Request Size Limits
POST body and multipart uploads are capped at **15 MB** (Tomcat + Spring multipart config) to prevent DoS via oversized payloads. The limit is set above the 10 MB image cap to account for Base64 encoding overhead (~33%) when images are transmitted as JSON strings.

### Cross-Origin Opener Policy
`SecurityConfig` sets `Cross-Origin-Opener-Policy: same-origin-allow-popups` to isolate the browsing context while allowing Google OAuth popup flows.

### Rate Limiting
In-memory sliding window limiter, keyed by `endpoint + client IP`.

| Endpoint | Limit |
|----------|-------|
| POST `/api/auth/login` | 10 requests / 60 seconds |
| POST `/api/auth/signup` | 5 requests / 60 seconds |
| POST `/api/auth/forgot-password` | 3 requests / 5 minutes |
| POST `/api/auth/reset-password` | 10 requests / 60 seconds |
| POST `/api/auth/change-password` | 5 requests / 60 seconds |

Returns `HTTP 429 Too Many Requests` when exceeded.

### Password Reset Tokens
- Tokens are UUIDs stored in `mystuff.password_reset_token`.
- Expire after 1 hour (configurable via `mystuff.reset.token-ttl-minutes`).
- Single-use: marked `used = true` after a successful password reset.
- The forgot-password endpoint always returns 200 to prevent email enumeration — it never reveals whether an address is registered.
- When the email is not registered, the endpoint sleeps 200–500 ms to prevent timing-based enumeration.

### Support Token
A support request requires two steps:
1. Verify QR ownership → receive a time-limited token
2. Submit the request with that token

Prevents spam and unauthorised support requests.

### Frontend Login Lockout
The frontend tracks failed login attempts per email in `localStorage`. After **3 consecutive failures**, a 30-second cooldown is enforced client-side and a support/unlock form is shown. The user can scan their QR code to submit a support request for account help.

## 6. Development Setup

### Prerequisites
- Docker + Docker Compose
- Java 21 (for local runs without Docker)

### Environment Variables (`.env`)

Copy `.env.example` to `.env` and fill in the required values:

```
DB_USERNAME=...
DB_PASSWORD=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...

# Mailtrap — used to send password reset emails
# Sign up at https://mailtrap.io (free tier is sufficient for local dev)
MAILTRAP_API_TOKEN=<your-mailtrap-api-token>
MAILTRAP_FROM_EMAIL=hello@demomailtrap.co
MAILTRAP_FROM_NAME=Tagly

# Base URL for the reset link included in emails
RESET_LINK_BASE=http://localhost:8081/reset-password

# Set to true only when the server runs behind HTTPS
SESSION_COOKIE_SECURE=false
```

### Run with Docker (recommended)

```bash
# Start database and backend
docker compose up -d

# View logs
docker logs -f team2-backend
docker logs -f team2-postgres

# Reset database (drops all data)
docker compose down -v
docker compose up -d
```

The backend is available at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Run Locally (without Docker)

1. Start a PostgreSQL instance on port 5432
2. Create the schema: run `database/2_create.sql`
3. Seed data: run `database/3_import.sql`
4. Configure `src/main/resources/application.properties` with your DB credentials
5. Run: `./gradlew bootRun`

### Seed Data (from `database/3_import.sql`)

| Username | Password | Role |
|----------|----------|------|
| admin | 123 | admin |
| hanna | 123 | customer |
| katha | 123 | customer |

### Running Tests

Tests use [Testcontainers](https://testcontainers.com/), which spins up a real PostgreSQL 16 container — no manual database setup needed. Requires Docker to be running.

Use the provided script, which also clears stale Gradle lock files before running (prevents `Timeout waiting to lock file hash cache` errors after a crashed build):

```bash
# Run all tests
./run-tests.sh

# Run a single test class
./run-tests.sh --tests "ee.valiit.mystuffback.controller.item.ItemControllerTest"
```

Any extra arguments are passed through to `./gradlew test` directly. To invoke Gradle manually:

```bash
./gradlew test
```

Test results are written to `build/reports/tests/test/index.html`.

### Frontend Setup

**Prerequisites:** Node.js (LTS recommended), npm

```bash
# Install dependencies
npm install

# Start development server (http://localhost:8081)
npm run serve

# Build for production
npm run build
```

The dev server proxies all API requests to `http://localhost:8080` (configured in `vue.config.js`), so the backend must be running before using the frontend.

**Frontend available at:** `http://localhost:8081`

#### Google OAuth
The frontend uses Google Sign-In. The Google Client ID is configured in `src/views/LoginView.vue`.

#### Session Storage
After login, the frontend stores `userId`, `roleName`, and `username` in `sessionStorage`. These are cleared on logout.

## 7. Roadmap & Team Tasks

| # | Feature | Status | Notes |
|---|---------|--------|-------|
| 1 | **Login redesign** | Done | Switched to email-based login |
| 2 | **Google OAuth** | Done | `POST /api/auth/google` with Google ID token |
| 3 | **Password Reset** | Done | `POST /api/auth/forgot-password`, `/reset-password`, `/change-password`; Mailtrap email delivery |
| 4 | **Testing** | Done | Integration tests via Testcontainers; unit tests with Mockito |
| 5 | **Push notifications** | Planned | Notify users of important events |
| 6 | **Warranty expiry emails** | Planned | Cron job — email when warranty is about to expire or has expired |
| 7 | **Family & Friends sharing** | Planned | Share item access with other users |
| 8 | **Admin panel** | Planned | Dashboard for admin users to manage accounts and requests |
| 9 | **New frontend design** | Planned | UI redesign |
| 10 | **Deploy to TalTech server** | Planned | Production deployment |

## 8. Development Guidelines

### API-First
- Design endpoints before writing service or repository code
- Use OpenAPI/Swagger annotations to define the contract
- The Swagger UI at `/swagger-ui.html` is the live contract between backend and frontend

### Code Conventions
- Controllers: thin — only handle HTTP concerns and call services
- Services: all business logic lives here
- Repositories: only data access, no logic
- DTOs: use MapStruct for entity ↔ DTO conversion
- Never hard-delete data — use `status` flags for soft deletes
