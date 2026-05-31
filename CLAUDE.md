# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ZenoEats is a food delivery platform built as a Gradle multi-module monorepo. The project is early-stage; `restaurant-service` has a full CRUD implementation while `user-service` is an empty skeleton awaiting auth implementation (see `docs/ROADMAP.md` for the 8-week plan).

## Tech Stack

- **Java 21**, **Spring Boot 3.5.7**, **Gradle 8.14.3**
- **Persistence**: JPA/Hibernate with MariaDB; `ddl-auto: update` (schema auto-evolves on startup)
- **Mapping**: MapStruct 1.6.3 for DTO↔Entity conversion
- **Infrastructure**: Docker Compose — MariaDB (3306) and Redis (6379) in `infra/`
- **API Testing**: Bruno collections in `api-collections/`

## Commands

```bash
# Start infrastructure (run from infra/ or use the full path)
docker compose -f infra/docker-compose.yaml up -d

# Build all modules
./gradlew build

# Build without tests
./gradlew build -x test

# Run a specific service
./gradlew services:restaurant-service:bootRun
./gradlew services:user-service:bootRun

# Run tests
./gradlew test
./gradlew services:restaurant-service:test
./gradlew test --tests "*RestaurantTest"
```

## Architecture

### Module Layout

```
zenoeats/
├── services/
│   ├── restaurant-service/   # Full CRUD, port 8082
│   └── user-service/         # Skeleton only, port 8080
├── shared/
│   └── common-api/           # Shared response DTOs (java-library)
└── infra/
    ├── docker-compose.yaml
    └── .env.example           # Copy to .env before running
```

### Dependency Graph

All services depend on `:shared:common-api` for shared types. `common-api` is a `java-library` and exports Jackson, Lombok, and Jakarta validation transitively.

### Shared Response Contract

Every endpoint wraps its result in `ApiResponse<T>` from `shared/common-api`:
```java
ApiResponse.success("Restaurant created", dto);   // 2xx
ApiResponse.error("Not found");                    // 4xx/5xx
```

`ErrorCode` enum (VALIDATION_FAILED, NOT_FOUND, etc.) and `ApiErrorUtils` for extracting field-level validation errors are in `shared/common-api/src/main/java/com/zenoeats/shared/`.

### Restaurant Service Layers

`RestaurantController` → `RestaurantService` (interface) → `RestaurantServiceImpl` → `RestaurantRepository` (JPA). `RestaurantMapper` (MapStruct) handles `RestaurantRequest`/`RestaurantResponse` ↔ `Restaurant` entity. `GlobalExceptionHandler` (`@ControllerAdvice`) catches `RestaurantNotFoundException`.

`Restaurant` has a `@OneToMany` relationship to `MenuItem`.

### Known Issues

- Package has a typo: `restauarantservice` (double 'a') — leave as-is unless doing a full rename.
- `GlobalExceptionHandler` in restaurant-service returns a raw `Map`, not `ApiResponse`. Should be unified with the shared pattern when touched.
- `application.yaml` in restaurant-service has a hardcoded DB password — use env vars when implementing auth or production config.

## Environment Setup

Copy `infra/.env.example` to `infra/.env` and fill in DB credentials. The `application.yaml` for restaurant-service reads `spring.datasource` directly — ensure MariaDB is running before `bootRun`.
