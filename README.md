MyStuffLabelled – Backend 
Backend REST API for MyStuffLabelled, a personal inventory system that enables users to store item metadata and access it via QR code.

## Features
- User authentication and authorization
- CRUD operations for items
- Receipt image upload
- QR-based item access
- User-specific data isolation

## Security
- Request limiting
- Input validation

## Tech Stack
- Java
- Spring Boot
- Gradle
- PostgreSQL

## Frontend Repository
https://github.com/KatharinaMat/mystufffront

## Prerequisites

### To run the app
- **Docker Desktop** (includes Docker Compose) — https://www.docker.com/products/docker-desktop
- **Git**

### For VS Code development
These are only needed so VS Code understands the Java code (fixes red file errors). Not required to run the app.
- **JDK 21** — https://adoptium.net/temurin/releases/?version=21
- **VS Code extension**: `vscjava.vscode-java-pack` (Java Extension Pack)

## .env Setup

The `.env` file is not included in the repository. Create it manually in the project root before running Docker:

```
GOOGLE_CLIENT_ID=<your-google-client-id>
GOOGLE_CLIENT_SECRET=<your-google-client-secret>
DB_USERNAME=<your-db-username>
DB_PASSWORD=<your-db-password>
```

Google OAuth credentials can be obtained from the project owner or from Google Cloud Console.

## Local Development
### Run the docker container with:
````
docker compose up -d
````
This will run postgre database and Spring Boot

### Logs are visible here:
````
docker logs -f team2-backend
````
````
docker logs -f team2-postgres
````

### To clear the database and start new:
````
docker compose down -v
````
````
docker compose up -d
````

## Running Tests

Requires **Docker Desktop** to be running.

````
./run-tests.sh
````

Run a single test class:
````
./run-tests.sh --tests "ee.valiit.mystuffback.controller.item.ItemControllerTest"
````