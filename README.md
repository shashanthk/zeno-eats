# ZenoEats

A food delivery platform built as a Gradle multi-module monorepo to demonstrate production-grade
microservices patterns — JWT auth, event-driven flows, distributed transactions, observability,
and resilience.

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the full architecture, decisions log, and remaining
roadmap.

---

## Services

| Service | Port | Database | Status |
|---|---|---|---|
| `user-service` | 8080 | PostgreSQL (5432) | Auth (register / login / me) |
| `restaurant-service` | 8082 | PostgreSQL (5433) | Full CRUD |

---

## Prerequisites

- Java 21
- Docker Desktop
- Gradle 8.14.3 (or use the `./gradlew` wrapper)
- Git Bash, WSL, or macOS Terminal (for key generation — PowerShell cannot run the `openssl` pipe commands below)

---

## Local Setup

### 1. Clone and copy environment config

```bash
git clone <repo-url>
cd zenoeats
cp infra/.env.example infra/.env
```

### 2. Generate the RSA key pair for JWT signing

`user-service` signs JWT tokens with an RSA private key (RS256). The keys are stored as
Base64-encoded DER strings in `infra/.env` — they are never committed to the repository.

Run these commands from the repo root in **Git Bash, WSL, or a Unix terminal**:

```bash
# Step 1 — generate a 2048-bit RSA private key
openssl genrsa -out jwt_private.pem 2048

# Step 2 — export the private key as Base64-encoded PKCS8 DER → paste into JWT_PRIVATE_KEY
openssl pkcs8 -topk8 -nocrypt -in jwt_private.pem -outform DER | base64 -w 0

# Step 3 — export the public key as Base64-encoded DER → paste into JWT_PUBLIC_KEY
openssl rsa -in jwt_private.pem -pubout -outform DER | base64 -w 0

# Step 4 — delete the raw PEM file (the Base64 values in .env are all you need)
rm jwt_private.pem
```

Copy the output of Step 2 into `JWT_PRIVATE_KEY` and Step 3 into `JWT_PUBLIC_KEY` inside
`infra/.env`. The file should look like:

```dotenv
JWT_PRIVATE_KEY=MIIEvQIBADANBgkqhkiG9w0BAQE...   # long Base64 string
JWT_PUBLIC_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOC...    # long Base64 string
JWT_EXPIRATION_MS=86400000
```

> **Why RS256?** Only `user-service` holds the private key and signs tokens. Every other
> service verifies tokens using the public key only — a leaked public key cannot forge tokens.
> See `docs/ROADMAP.md` (AD-4) for the full reasoning.

### 3. Fill in the remaining `.env` values

Open `infra/.env` and fill in the database credentials:

```dotenv
# MariaDB — restaurant-service
MYSQL_ROOT_PASSWORD=choose-a-root-password
MYSQL_DATABASE=zeno_eats_db
MYSQL_USER=zeno_app_user
MYSQL_PASSWORD=choose-a-password

# Redis
REDIS_PASSWORD=choose-a-password

# restaurant-service reads these at startup
DB_PASSWORD=same-as-MYSQL_PASSWORD

# PostgreSQL — user-service
USER_DB_NAME=zeno_users_db
USER_DB_USERNAME=zeno_user_app
USER_DB_PASSWORD=choose-a-password
```

`DB_URL` and `USER_DB_URL` already have defaults in `application.yaml` — only override them
if you change ports or hostnames.

### 4. Start infrastructure

Use the helper script in `scripts/` — no need to remember the full `docker compose` path:

```bash
chmod +x scripts/infra.sh      # first time only
./scripts/infra.sh start
```

This starts MariaDB (3306), PostgreSQL (5432), and Redis (6379). All three are on the
`zeno-eats-network` Docker network so they resolve each other by container name.

The PostgreSQL `users` table is created automatically from `infra/init/user-service/01_schema.sql`
on the first startup. If you need a clean slate (re-runs init scripts):

```bash
./scripts/infra.sh reset
```

### 5. Build and run

```bash
# Build all modules
./gradlew build -x test

# Run a service
./gradlew services:user-service:bootRun
./gradlew services:restaurant-service:bootRun
```

The root `build.gradle` configures `bootRun` to automatically read `infra/.env` and inject
all variables as environment variables before the service starts. Spring Boot's `${VAR}`
placeholders in `application.yaml` resolve against these — no IDE-specific run configuration
needed.

Hibernate creates tables automatically on first startup (`ddl-auto: update`).

---

## API Overview

### user-service — `http://localhost:8080`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | None | Register a new user, returns JWT |
| `POST` | `/api/v1/auth/login` | None | Login, returns JWT |
| `GET` | `/api/v1/users/me` | Bearer token | Returns current user profile |

All responses use the shared `ApiResponse<T>` envelope:

```json
// success
{ "status": true, "message": "...", "data": { ... } }

// error
{ "status": false, "message": "...", "error": { "code": "...", "fieldErrors": [...] } }
```

### restaurant-service — `http://localhost:8082`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/restaurants` | Create restaurant (201) |
| `GET` | `/api/v1/restaurants` | List all restaurants |
| `GET` | `/api/v1/restaurants/{id}` | Get restaurant by ID |
| `PUT` | `/api/v1/restaurants/{id}` | Update restaurant |
| `DELETE` | `/api/v1/restaurants/{id}` | Delete restaurant |

---

## API Testing (Bruno)

[Bruno](https://www.usebruno.com/) collections are in `api-collections/`.

1. Open Bruno and load the `api-collections/` folder as a collection
2. Select the `ZENO_ENV` environment
3. Run requests in order — the **Register** and **Login** requests automatically save the
   JWT token into `AUTH_TOKEN`, which **Get Current User** picks up via bearer auth

---

## Build Reference

```bash
./gradlew build                              # build all modules + tests
./gradlew build -x test                      # build without tests
./gradlew test                               # run all tests
./gradlew services:user-service:test         # test one service
./gradlew test --tests "*SomeTest"           # run a specific test class
./scripts/infra.sh start          # start all containers
./scripts/infra.sh stop           # stop, keep volumes
./scripts/infra.sh restart        # stop then start
./scripts/infra.sh reset          # stop + wipe volumes (re-runs init scripts)
./scripts/infra.sh status         # show container status
./scripts/infra.sh logs           # tail all logs
./scripts/infra.sh logs user-db   # tail PostgreSQL logs only
```
