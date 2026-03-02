MyStuffLabelled – Backend 
Backend REST API for MyStuffLabelled, a personal inventory system that enables users to store item metadata and access it via QR code.

## Features
- User authentication and authorization
- CRUD operations for items
- Receipt image upload
- QR-based item access
- User-specific data isolation

## Security
- Honeypot protection
- Request limiting
- Input validation

## Tech Stack
- Java
- Spring Boot
- Gradle
- PostgreSQL

## Frontend Repository
https://github.com/KatharinaMat/mystufffront

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