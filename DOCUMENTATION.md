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

---

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

---

## 2. Architecture

### Layer Overview

```
Request
  └── Controller (REST, input validation)
        └── Service (business logic)
              └── Repository (JPA, PostgreSQL)
```

### Package Structure

```
src/main/java/ee/valiit/mystuffback/
├── controller/          REST endpoints + DTOs
│   ├── item/
│   ├── login/
│   ├── qrcode/
│   ├── support/
│   └── user/
├── service/             Business logic
├── persistence/         JPA entities + repositories + mappers
│   ├── item/
│   ├── itemimage/
│   ├── role/
│   ├── support/
│   └── user/
└── infrastructure/      Cross-cutting concerns
    ├── config/          (PasswordConfig, CorsConfig)
    ├── error/           (GlobalExceptionHandler, error codes)
    ├── exception/       (custom exceptions)
    └── util/
```

### CORS
Allowed origins: `http://localhost:8081`, `http://localhost:8082`
Allowed methods: GET, POST, PUT, DELETE, OPTIONS
Credentials: allowed

---

## 3. Database Schema

Schema name: `mystuff`
Files: [database/2_create.sql](database/2_create.sql), [database/3_import.sql](database/3_import.sql)

### Entity-Relationship Overview

```
role ──< user ──< item ──< image
```

| Relationship | Type | Description |
|---|---|---|
| role → user | 1:N | A role has many users |
| user → item | 1:N | A user owns many items |
| item → image | 1:N | An item has many images |

### Tables

#### `mystuff.role`
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | serial | PK | |
| name | varchar(20) | NOT NULL | `'admin'` or `'customer'` |

#### `mystuff.user`
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | serial | PK | |
| role_id | int | FK → role.id | |
| username | varchar(255) | NOT NULL | |
| password | varchar(255) | NOT NULL | BCrypt hash |
| email | varchar(255) | NOT NULL | |
| status | varchar(1) | NOT NULL | `'A'` = active |

#### `mystuff.item`
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | serial | PK | |
| user_id | int | FK → user.id, NOT NULL | |
| name | varchar(50) | NOT NULL | |
| date | date | NOT NULL | Purchase/acquisition date |
| model | varchar(250) | NULL | Optional model number |
| comment | varchar(500) | NULL | Notes, warranty info, etc. |
| status | varchar(1) | NOT NULL | `'A'` = active, `'D'` = deleted |
| qr_token | varchar(255) | NULL, UNIQUE | UUID assigned when QR is generated |

#### `mystuff.image`
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | serial | PK | |
| item_id | int | FK → item.id, NOT NULL | |
| image_data | bytea | NOT NULL | Receipt or item photo (binary) |

### Indexes
| Index | Table | Column | Type |
|-------|-------|--------|------|
| ix_item_user_id | item | user_id | Regular |
| ix_image_item_id | image | item_id | Regular |
| ux_item_qr_token | item | qr_token | Unique |

### Soft Delete
Items are never hard-deleted. Setting `status = 'D'` hides the item from all queries. This preserves historical data and supports potential future audit/restore features.

---

## 4. API Reference

Interactive docs available at: `http://localhost:8080/swagger-ui.html`

---

### Authentication

#### `POST /api/auth/signup`
Register a new user account.

**Rate limit:** 5 requests / 60 seconds per IP

**Request body:**
```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "captchaToken": "string",
  "website": ""
}
```
> `website` is a honeypot field — must be empty. Do not display it in the UI.

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Account created |
| 400 | Validation error or CAPTCHA failed |
| 409 | Username already taken (error code 222) |
| 429 | Too many requests |

---

#### `POST /api/auth/login`
Authenticate and receive user identity.

**Rate limit:** 10 requests / 60 seconds per IP

**Request body:**
```json
{
  "username": "string",
  "password": "string",
  "website": ""
}
```

**Response (200):**
```json
{
  "userId": 1,
  "roleName": "customer"
}
```

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Login successful |
| 403 | Wrong credentials (error code 111) |
| 429 | Too many requests |

---

### Items

#### `POST /item`
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
| 200 | Item created |
| 409 | Item name already exists for this user (error code 333) |

---

#### `GET /items`
Get all active items for a user.

**Query params:** `userId` (required)

**Response (200):**
```json
[
  {
    "itemId": 1,
    "itemName": "Washing Machine",
    "date": "2022-05-10"
  }
]
```

---

#### `GET /item`
Get full details of a single item, including images.

**Query params:** `itemId` (required)

**Response (200):**
```json
{
  "itemId": 1,
  "itemName": "Washing Machine",
  "date": "2022-05-10",
  "model": "Bosch WAX28EH0",
  "comment": "5-year warranty, receipt attached",
  "imageData": "base64-encoded-string",
  "status": "A"
}
```

---

#### `PUT /item`
Update an item's details.

**Query params:** `itemId` (required)

**Request body:** Same fields as POST `/item`

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Updated |
| 404 | Item not found |

---

#### `DELETE /item`
Soft-delete an item (sets `status = 'D'`).

**Query params:** `itemId` (required)

---

#### `DELETE /{itemId}/images/{imageId}`
Remove a specific image from an item.

**Path params:** `itemId`, `imageId`

---

### QR Codes

#### `GET /qr-code`
Get the QR code URL for an item. Assigns a `qr_token` to the item if it doesn't have one yet.

**Query params:** `itemId` (required)

**Response (200):**
```json
{
  "qrUrl": "http://localhost:8081/item?itemId=1&t=550e8400-e29b-41d4-a716-446655440000"
}
```

---

### Support

#### `POST /api/support/verify-qr`
Verify that the caller owns the item referenced by a QR token. Issues a short-lived support token.

**Request body:**
```json
{
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

---

#### `POST /api/support/request`
Submit a support request to the admin. Requires a valid support token from `/verify-qr`.

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

---

### Error Response Format

All errors follow this structure:
```json
{
  "message": "Human-readable description",
  "errorCode": 111
}
```

| Error Code | Meaning |
|------------|---------|
| 111 | Incorrect username or password |
| 222 | Username already taken |
| 333 | Item name already exists for this user |

---

## 5. Security

### Password Hashing
All passwords are stored as BCrypt hashes. Accounts with legacy plaintext passwords are automatically re-hashed to BCrypt on successful login.

### Rate Limiting
In-memory sliding window limiter, keyed by `endpoint + client IP`.

| Endpoint | Limit |
|----------|-------|
| POST `/api/auth/login` | 10 requests / 60 seconds |
| POST `/api/auth/signup` | 5 requests / 60 seconds |

Returns `HTTP 429 Too Many Requests` when exceeded.

### Honeypot
Login, signup, and support endpoints include an optional `website` field. Any non-empty value causes the request to be rejected silently. This field must never be shown in the UI.

### CAPTCHA
Signup requires a valid hCaptcha token (`captchaToken`). The backend verifies it server-side before creating the account.

### Support Token
A support request requires two steps:
1. Verify QR ownership → receive a time-limited token
2. Submit the request with that token

Prevents spam and unauthorised support requests.

---

## 6. Development Setup

### Prerequisites
- Docker + Docker Compose
- Java 21 (for local runs without Docker)

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

---

## 7. Roadmap & Team Tasks

### Current Sprint — Developer Task Assignments

| Developer | Task |
|-----------|------|
| Kerly | Login redesign / authentication rework |
| Kristjan | Server deployment (TalTech) |
| Katharina | Frontend migration Vue → Angular + CSS |
| Raido | APIs, database changes, admin view |

### Full Backlog

| # | Feature | Notes |
|---|---------|-------|
| 1 | **Login redesign** | Rework authentication flow |
| 2 | **Google OAuth** | Allow sign-in with Google account |
| 3 | **Flutter mobile app** | Native mobile client |
| 4 | **Push notifications** | Notify users of important events |
| 5 | **Warranty expiry emails** | Cron job — email when warranty is about to expire or has expired |
| 6 | **Family & Friends sharing** | Share item access with other users |
| 7 | **Frontend: Vue → Angular** | Migrate the frontend framework |
| 8 | **Admin panel** | Dashboard for admin users to manage accounts and requests |
| 9 | **New frontend design** | UI redesign |
| 10 | **Deploy to TalTech server** | Production deployment |
| 11 | **Testing** | Unit and integration tests |

---

## 8. Development Guidelines

Based on the team's agreed principles:

### API-First
Design your endpoints **before** writing service or repository code. Use OpenAPI/Swagger annotations to define the contract. The frontend team depends on it.

### Document Before You Code
Write or update this documentation when you add or change an endpoint, table, or feature. Documentation is a deliverable, not an afterthought.

### Database Design First
Draw the ER diagram and agree on the schema **before** writing any Java code. Mistakes in the schema are expensive to fix later. Every relationship has a pattern:
- **1:1** — use a foreign key on either side
- **1:N** — foreign key on the "many" side
- **M:N** — use a join/bridge table

Normalise to **3NF** as a baseline. Avoid storing derived or redundant data.

### OpenAPI = Team Contract
The Swagger UI at `/swagger-ui.html` is the live contract between backend and frontend. Keep it up to date. Frontend devs should not need to read Java code to understand the API.

### Code Conventions
- Controllers: thin — only handle HTTP concerns and call services
- Services: all business logic lives here
- Repositories: only data access, no logic
- DTOs: use MapStruct for entity ↔ DTO conversion
- Never hard-delete data — use `status` flags for soft deletes
