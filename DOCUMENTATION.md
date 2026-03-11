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
```

| Relationship | Type | Description |
|---|---|---|
| role → user | 1:N | A role has many users |
| user → item | 1:N | A user owns many items |
| item → image | 1:N | An item has many images |

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
| 200 | Account created |
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

#### `GET /items`
Get all active items for a user.

**Query params:** `userId` (required)

#### `GET /item`
Get full details of a single item, including images.

**Query params:** `itemId` (required)

#### `PUT /item`
Update an item's details.

**Query params:** `itemId` (required)

**Request body:** Same fields as POST `/item`

**Responses:**
| Status | Meaning |
|--------|---------|
| 200 | Updated |
| 404 | Item not found |

#### `DELETE /item`
Soft-delete an item (sets `status = 'D'`).

**Query params:** `itemId` (required)

#### `DELETE /{itemId}/images/{imageId}`
Remove a specific image from an item.

**Path params:** `itemId`, `imageId`

### QR Codes

#### `GET /qr-code`
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

## 5. Security

### Password Hashing
All passwords are stored as BCrypt hashes. Legacy plaintext passwords are automatically re-hashed on first login.

### Rate Limiting
In-memory sliding window limiter, keyed by `endpoint + client IP`.

| Endpoint | Limit |
|----------|-------|
| POST `/api/auth/login` | 10 requests / 60 seconds |
| POST `/api/auth/signup` | 5 requests / 60 seconds |

Returns `HTTP 429 Too Many Requests` when exceeded.

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
| 3 | **Push notifications** | Planned | Notify users of important events |
| 4 | **Warranty expiry emails** | Planned | Cron job — email when warranty is about to expire or has expired |
| 5 | **Family & Friends sharing** | Planned | Share item access with other users |
| 6 | **Admin panel** | Planned | Dashboard for admin users to manage accounts and requests |
| 7 | **New frontend design** | Planned | UI redesign |
| 8 | **Deploy to TalTech server** | Planned | Production deployment |
| 9 | **Testing** | Planned | Unit and integration tests |

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
