# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ZenoEats is a food delivery platform built as a Gradle multi-module monorepo. See `docs/ROADMAP.md` for the full architecture vision, decisions log, current state per service, and remaining roadmap. Reading `CLAUDE.md` + `docs/ROADMAP.md` is enough to resume work cold.

## Tech Stack

- **Java 21**, **Spring Boot 3.5.7**, **Gradle 8.14.3**
- **Persistence**: JPA/Hibernate — all services use PostgreSQL; `ddl-auto: update`
- **Security**: Spring Security 6, stateless JWT (RS256 via JJWT 0.12.x)
- **Mapping**: MapStruct 1.6.3
- **Infrastructure**: Docker Compose — PostgreSQL restaurant-db (5433), PostgreSQL user-db (5432), Redis (6379) in `infra/`
- **API Testing**: Bruno collections in `api-collections/`

## Commands

```bash
# Start all infrastructure
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
│   ├── restaurant-service/   # Full CRUD, port 8082, MariaDB
│   └── user-service/         # Auth (register/login/me), port 8080, PostgreSQL
├── shared/
│   └── common-api/           # Shared response DTOs (java-library)
├── docs/
│   └── ROADMAP.md            # Architecture decisions, current state, remaining plan
└── infra/
    ├── docker-compose.yaml
    └── .env.example           # Copy to .env before running
```

### Dependency Graph

All services depend on `:shared:common-api`. It is a `java-library` and exports Jackson, Lombok, and Jakarta validation transitively. Dependency versions are managed by the Spring BOM in the root `build.gradle` — never pin versions in subproject build files unless the BOM does not cover them.

### Shared Response Contract

Every endpoint — success and error — wraps its result in `ApiResponse<T>` from `shared/common-api`:
```java
ApiResponse.success("Restaurant created", dto);        // 2xx
ApiResponse.error("Not found");                        // 4xx/5xx
ApiResponse.error("Validation failed", apiError);      // 400 with field errors
```

`ErrorCode` enum, `ApiError`, and `ApiErrorUtils.fromBindingResult()` are in `shared/common-api/src/main/java/com/zenoeats/shared/`.

Every service must have a `@RestControllerAdvice GlobalExceptionHandler` that handles at minimum:
- Domain-specific exceptions → `ApiResponse.error()`
- `MethodArgumentNotValidException` → `ApiResponse.error()` with `ApiErrorUtils`

### Restaurant Service Layers

`RestaurantController` → `RestaurantService` (interface) → `RestaurantServiceImpl` → `RestaurantRepository` (JPA).
`RestaurantMapper` (MapStruct) handles DTOs ↔ entity including `@MappingTarget` for updates.
`Restaurant` has `@OneToMany` to `MenuItem`.

### User Service Layers

`AuthController` / `UserController` → `AuthService` → `AuthServiceImpl` → `UserRepository`.

Security filter chain (every request):
```
Request → JwtAuthenticationFilter → controller
              ↓
          extract Bearer token
          JwtService.isTokenValid()  (verifies RS256 signature with RSAPublicKey)
          load User from DB
          set SecurityContextHolder  ← @AuthenticationPrincipal reads from here
```

`RsaKeyConfig` loads `RSAPrivateKey` + `RSAPublicKey` from Base64-encoded env vars at startup.
`JwtService` signs with the private key, verifies with the public key (JJWT 0.12.x API).
`SecurityConfig` uses `DaoAuthenticationProvider(UserDetailsService)` (Spring Security 6.4 API).

### Entity Design Rules

Never use `@Data` on JPA entities. Use:
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = "collectionField")   // if @OneToMany/@ManyToMany present
@EqualsAndHashCode(of = "id")
```
Use `@Builder.Default` for any field with an initializer — Lombok `@Builder` ignores field
initializers without it, producing silent `null` values on mapped entities.

### Service Layer Rules

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // class-level default for reads
public class MyServiceImpl implements MyService {

    @Override
    @Transactional                 // override for all write methods
    public Dto create(Request req) { ... }
}
```

Hibernate dirty-checks managed entities at transaction commit — no redundant `save()` call
needed after mutating an entity fetched within the same transaction.

### Known Issues

- Package typo in restaurant-service: `restauarantservice` (double 'a') — leave as-is unless doing a full rename.

## Environment Setup

Copy `infra/.env.example` to `infra/.env` and fill in all variables. The example file contains
inline comments for every variable including how to generate the RS256 JWT key pair:

```bash
# Generate RSA key pair (run in Git Bash / WSL / macOS Terminal)
openssl genrsa -out jwt_private.pem 2048
openssl pkcs8 -topk8 -nocrypt -in jwt_private.pem -outform DER | base64 -w 0   # → JWT_PRIVATE_KEY
openssl rsa -in jwt_private.pem -pubout -outform DER | base64 -w 0              # → JWT_PUBLIC_KEY
```

Start infrastructure before `bootRun`:
```bash
docker compose -f infra/docker-compose.yaml up -d
```
