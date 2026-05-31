# ZenoEats — Project Roadmap & State

This document is the single source of truth for project context, decisions, and remaining work.
It is written for both the developer and future Claude Code sessions — reading this file plus
`CLAUDE.md` is enough to resume work cold.

---

## Target Architecture

A distributed microservices food delivery platform demonstrating production-grade patterns.

```
                        ┌─────────────────┐
         Clients ──────▶│   API Gateway   │ (Spring Cloud Gateway)
                        └────────┬────────┘
                                 │ routes + enforces auth
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                  ▼
       ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
       │ user-service│   │ restaurant  │   │order-service│
       │  (auth +    │   │  -service   │   │(saga +      │
       │  profiles)  │   │ (menus +    │   │ events)     │
       │  PostgreSQL │   │  cache)     │   │ PostgreSQL  │
       └─────────────┘   │ PostgreSQL  │   └──────┬──────┘
                         └─────────────┘          │
                                                  │ Kafka events
                         ┌────────────────────────┼──────────────────┐
                         ▼                        ▼                  ▼
                  ┌─────────────┐        ┌─────────────┐   ┌─────────────────┐
                  │  payment-   │        │  delivery-  │   │ notification-   │
                  │  service    │        │  service    │   │ service         │
                  │ PostgreSQL  │        │ PostgreSQL  │   │ (event consumer)│
                  └─────────────┘        └─────────────┘   └─────────────────┘
```

**Supporting infrastructure:** Redis (hot reads), Kafka (async events), Config Server
(centralised config), Service Discovery (Kubernetes DNS or Eureka).

**Observability stack:** OpenTelemetry → Zipkin (tracing), Prometheus + Grafana (metrics),
Loki (logs).

**Resilience:** Resilience4j — circuit-breaker, bulkhead, retry, timeout on every
inter-service call.

**Deployment:** Docker Compose for local demo → Kubernetes (minikube/kind) for production
demo → GitHub Actions CI/CD.

---

## Architecture Decisions

These are the deliberate choices made during implementation and the reasoning behind each.
Understand these before suggesting changes.

### AD-1 — Gradle multi-module monorepo
All services live in one repo under `services/`, sharing a root `build.gradle` for version
management. The root `subprojects {}` block sets Java 21, Spring BOM, and test dependencies
once. Individual service `build.gradle` files only declare what is unique to them.
**Why:** Simpler CI, shared library versioning, visible cross-service impact of changes.

### AD-2 — Shared `common-api` library
Every service depends on `:shared:common-api` (a `java-library` plugin module). It exports
`ApiResponse<T>`, `ApiError`, `ErrorCode`, and `ApiErrorUtils` as the universal API envelope.
**Why:** All success and error responses across every service must have the same JSON shape so
API Gateway and clients parse one format. Jackson, Lombok, and Jakarta validation are exported
transitively via `api` scope.

### AD-3 — One database per service (polyglot persistence)
Each service owns its own database and no other service touches it directly.
- `restaurant-service` → MariaDB (was the first service, started before the PostgreSQL decision)
- `user-service` → PostgreSQL
- All future services → PostgreSQL
**Why:** Data ownership boundary. Schema changes in one service do not break others.
`restaurant-service` should be migrated to PostgreSQL in a future cleanup session.

### AD-4 — RS256 JWT (asymmetric) over HS256 (symmetric)
`user-service` signs tokens with an RSA **private key**. Any other service verifies tokens
with the **public key** only.
**Why:** With HS256 every service needs the same secret — a compromised service can forge
tokens for the whole system. With RS256 only the signing service holds the private key;
a leaked public key is useless for forging.
**Future:** When `auth-service` is extracted, only the private key moves there. Other services
continue verifying with the public key, optionally fetched from a `/.well-known/jwks.json`
endpoint (standard JWKS pattern).

### AD-5 — Auth embedded in `user-service` (not a separate `auth-service`)
JWT issuance and user management are co-located in `user-service` for now.
**Why:** Simpler to build and reason about at this stage. The separation into `auth-service`
is architecturally valid but adds complexity before the patterns are fully understood.
**Migration path:** Extract `JwtService`, `RsaKeyConfig`, `SecurityConfig`, and the
`/auth/**` endpoints into a dedicated `auth-service`. Other services will use the public key
to verify tokens issued by that service.

### AD-6 — Stateless Spring Security filter chain
`SessionCreationPolicy.STATELESS` — no `HttpSession` is created or used. Every request must
carry a JWT in the `Authorization: Bearer <token>` header. `JwtAuthenticationFilter` (extends
`OncePerRequestFilter`) validates the token and populates `SecurityContextHolder` before the
request reaches any controller.
**Why:** Sessions cannot be shared across horizontally-scaled microservice instances without a
shared session store. Stateless tokens are self-contained and scale naturally.

### AD-7 — `@Transactional(readOnly = true)` at service class level
Service implementation classes are annotated `@Transactional(readOnly = true)` at the class
level. Write methods override with `@Transactional`. `readOnly = true` tells Hibernate to skip
dirty checking on reads, giving a small performance benefit and preventing accidental writes in
read-only methods.
**Why:** Explicit transactional boundaries; Hibernate dirty checking handles UPDATE SQL on
managed entities — no redundant `save()` calls needed in update methods.

### AD-8 — MapStruct for all DTO↔Entity mapping
`RestaurantMapper` (and future equivalents) use MapStruct with `componentModel = "spring"`.
Update operations use `@MappingTarget` so MapStruct generates the field-copy code — no manual
field assignment that silently breaks when new fields are added.

### AD-9 — No `@Data` on JPA entities
JPA entities use explicit Lombok annotations: `@Getter @Setter @Builder @NoArgsConstructor
@AllArgsConstructor @EqualsAndHashCode(of = "id")`. `@ToString(exclude = "<collection>")` is
added on any entity with a `@OneToMany` or `@ManyToMany` association.
**Why:** `@Data` generates `equals()`/`hashCode()` over all fields (breaks JPA identity
contract) and `toString()` that traverses lazy collections (silent N+1).

### AD-10 — `@Builder.Default` for entity field defaults
When Lombok `@Builder` is present, field initializers (e.g. `private Boolean active = true`)
are silently ignored by the builder unless annotated `@Builder.Default`. MapStruct uses the
builder path, so forgetting this causes default values to be `null` on every mapped entity.

### AD-11 — Immutable exception classes
Custom exceptions use `@Getter` only (not `@Data`). Structured fields (e.g. `id`, `email`) are
`final` and set only in the constructor. Exceptions must not be mutable after construction.

---

## Current State

### `shared/common-api` — Complete
| Class | Purpose |
|---|---|
| `ApiResponse<T>` | Universal envelope — `status`, `message`, `data`, `error` |
| `ApiError` | Error detail with `code`, `detail`, `fieldErrors` |
| `ErrorCode` | Enum — `VALIDATION_FAILED`, `NOT_FOUND`, `INTERNAL_ERROR`, `UNAUTHORIZED`, `FORBIDDEN` |
| `ApiErrorUtils` | Extracts `List<FieldError>` from Spring's `BindingResult` |

### `services/restaurant-service` — CRUD complete (port 8082, MariaDB)
| Layer | Status |
|---|---|
| Entity (`Restaurant`, `MenuItem`) | Done — correct JPA annotations, `@Builder.Default active = true` |
| `RestaurantMapper` | Done — `toEntity`, `toResponse`, `updateEntity` (`@MappingTarget`) |
| `RestaurantServiceImpl` | Done — `@Transactional(readOnly = true)`, write methods override |
| `RestaurantController` | Done — `POST` returns `201`, all errors via `ApiResponse` |
| `GlobalExceptionHandler` | Done — `404` + validation errors → `ApiResponse` |
| Redis caching | **Not yet** — planned in Week 3 |
| Menu versioning | **Not yet** — planned in Week 3 |
| PostgreSQL migration | **Not yet** — MariaDB still used, should align with other services |

### `services/user-service` — Auth complete (port 8080, PostgreSQL)
| Layer | Status |
|---|---|
| `User` entity | Done — implements `UserDetails`, `@Builder.Default role = CUSTOMER` |
| `UserRepository` | Done — `findByEmail`, `existsByEmail` |
| `RsaKeyConfig` | Done — loads RS256 key pair from Base64 env vars |
| `JwtService` | Done — RS256 sign (private key) / verify (public key), JJWT 0.12.x |
| `JwtAuthenticationFilter` | Done — validates token, sets `SecurityContextHolder` |
| `SecurityConfig` | Done — stateless, `DaoAuthenticationProvider(UserDetailsService)` |
| `AuthServiceImpl` | Done — register (BCrypt + save + issue token), login (delegate to `AuthenticationManager`) |
| `AuthController` | Done — `POST /api/v1/auth/register` (201), `POST /api/v1/auth/login` |
| `UserController` | Done — `GET /api/v1/users/me` (`@AuthenticationPrincipal`) |
| `GlobalExceptionHandler` | Done — `409 Conflict` (email exists), `401` (bad credentials), `400` (validation) |
| User profile update | **Not yet** |
| Password change | **Not yet** |
| Role-based access control | **Not yet** — `Role` enum exists, wired to authorities, no `@PreAuthorize` yet |

### `infra/docker-compose.yaml`
| Service | Image | Port | Volume |
|---|---|---|---|
| `zeno-mysql-db` | mariadb:12.0.2 | 3306 | `zeno-db-data` |
| `zeno-postgres-db` | postgres:16-alpine | 5432 | `zeno-user-db-data` |
| `zeno-redis-cache` | redis:alpine3.22 | 6379 | `zeno-redis-data` |

All services are on `zeno-eats-network` — containers resolve each other by container name.

---

## Established Code Patterns

Follow these consistently when adding new services or features.

**Response envelope — always wrap in `ApiResponse<T>`**
```java
// success
ResponseEntity.ok(ApiResponse.success("Message", dto));
ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created", dto));

// error (in GlobalExceptionHandler)
ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Not found"));
ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Validation failed", apiError));
```

**Exception handler — every service needs one**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // handle domain exceptions → ApiResponse.error()
    // handle MethodArgumentNotValidException → ApiResponse.error() with ApiErrorUtils
}
```

**Entity design**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = "collectionField")   // only if @OneToMany/@ManyToMany present
@EqualsAndHashCode(of = "id")
@Entity @Table(name = "table_name")
public class MyEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...
    @Builder.Default
    private Boolean someFlag = true;    // @Builder.Default required for field initializers
}
```

**Service layer**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)     // default for reads
public class MyServiceImpl implements MyService {

    @Override
    @Transactional                  // override for writes
    public Dto create(Request req) { ... }
}
```

**Custom exceptions**
```java
@Getter
public class MyNotFoundException extends RuntimeException {
    private final Long id;
    public MyNotFoundException(Long id) {
        super("X not found with id: " + id);
        this.id = id;
    }
}
```

**Credentials — always from env vars**
```yaml
# application.yaml
spring.datasource.password: ${DB_PASSWORD}   # no default — fail fast if missing
spring.datasource.url: ${DB_URL:jdbc:...}    # optional default for local dev
```

---

## Remaining Roadmap

Weeks 1–2 are complete. The plan below reflects actual current state.

### Week 3 — Restaurant service hardening + Redis cache
- Add Redis caching to `GET /restaurants` and `GET /restaurants/{id}` using `@Cacheable`
- Add `@CacheEvict` on create/update/delete
- Add menu versioning (version field on `Restaurant`, optimistic locking with `@Version`)
- Migrate `restaurant-service` from MariaDB to PostgreSQL (align all services)
- Add Bruno collection for `user-service` endpoints
- **Outcome:** Cached menu reads, concurrent update safety, all services on PostgreSQL

### Week 4 — Order Service + Saga pattern
- New `order-service` module (PostgreSQL, port 8083)
- `Order` entity with state machine: `PENDING → CONFIRMED → PAID → PREPARING → DISPATCHED → DELIVERED`
- Kafka integration — publish `order.created` event on confirmation
- Saga orchestration pattern — `order-service` drives the saga, sends commands, listens for replies
- **Outcome:** First inter-service event flow; demonstrates distributed transaction coordination

### Week 5 — Payment Service + Idempotency
- New `payment-service` module (PostgreSQL, port 8084)
- Sandbox payment processing — simulate success and failure paths
- Idempotency key support — clients pass `Idempotency-Key` header; duplicate requests return
  cached result without re-processing
- Compensating transaction — publish `payment.failed` event so `order-service` saga rolls back
- **Outcome:** Full order → payment flow with failure handling and compensation

### Week 6 — Delivery Service + Notification Service
- `delivery-service` (port 8085) — rider assignment stub, consumes `payment.succeeded`
- `notification-service` (port 8086) — subscribes to `order.*` and `payment.*` events,
  sends email/SMS stubs (console output or JavaMailSender stub)
- **Outcome:** Full async flow: order → payment → delivery → notification via Kafka

### Week 7 — Observability + Resilience
- Add Resilience4j — circuit-breaker + retry on all inter-service HTTP calls
- OpenTelemetry agent on all services → traces in Zipkin
- Prometheus `actuator/prometheus` endpoints + Grafana dashboard
- Loki log aggregation via Docker Compose
- **Outcome:** Visible distributed traces and metrics for a sample purchase end-to-end

### Week 8 — CI/CD + API Gateway + Auth-Service Extraction
- GitHub Actions CI — build all modules, run tests, build Docker images
- Add `api-gateway` (Spring Cloud Gateway) — route to services, enforce JWT at the gateway
- Extract `auth-service` from `user-service` (see AD-5) — move private key there,
  add `/.well-known/jwks.json`, update all services to verify against it
- k8s manifests or Helm charts for demo deploy (minikube)
- **Outcome:** Deployable, observable, production-shaped system

---

## Hiring-Focused Artifacts (produce alongside implementation)

| Artifact | Where |
|---|---|
| `docs/ARCHITECTURE.md` | Diagrams, trade-off analysis, sequence diagrams for order/payment/refund flows |
| `docs/RUNBOOK.md` | How to run locally end-to-end; how to deploy to k8s |
| Bruno API collections | `api-collections/` — one collection per service |
| Case study (1,200–1,500 words) | "ZenoEats — architecture decisions, trade-offs, and lessons" |
| LinkedIn thread | Link to repo + case study after Week 8 |

**Key interview talking points to be ready to defend:**
- Why one DB per service vs shared DB (data ownership, independent deployability)
- Why Saga over two-phase commit (availability over consistency, failure isolation)
- Why Kafka over synchronous REST for order flow (decoupling, durability, replay)
- Why RS256 over HS256 (private key confinement, JWKS future)
- How you'd handle a payment that succeeds but the `payment.succeeded` event never arrives

---

## Onboarding a New Claude Session

If context is lost, ask Claude to read in this order:

1. **`CLAUDE.md`** — project structure, build commands, module layout, known issues
2. **`docs/ROADMAP.md`** (this file) — decisions made, current state, what comes next
3. **Skim these files** for pattern reference:
   - `services/restaurant-service/src/main/java/.../service/impl/RestaurantServiceImpl.java`
   - `services/restaurant-service/src/main/java/.../exception/GlobalExceptionHandler.java`
   - `services/user-service/src/main/java/.../security/SecurityConfig.java`
   - `services/user-service/src/main/java/.../security/JwtService.java`
   - `shared/common-api/src/main/java/.../dto/ApiResponse.java`

After reading those five, the session has full context to continue any work in this repo.
