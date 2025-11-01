## High-level architecture

**ZenoEats** — a distributed microservices system with clear separation of concerns, event-driven flows, and production-grade observability.

* **API Gateway** (Spring Cloud Gateway) — single entry, routing, rate-limit, auth enforcement.
* **Auth Service** — JWT + optional Keycloak integration (separate for SSO demo).
* **Config Server** — centralized config for services (Spring Cloud Config or Kubernetes ConfigMaps).
* **Service Discovery** — use Kubernetes DNS (preferred) or Eureka for Compose demo.
* **Core Services (each with own DB):**

    * **User Service** (accounts, profiles) — PostgreSQL
    * **Restaurant Service** (menus, availability) — PostgreSQL
    * **Order Service** (order lifecycle, sagas) — PostgreSQL
    * **Delivery Service** (rider management, location) — PostgreSQL
    * **Payment Service** (sandbox integration) — PostgreSQL
    * **Notification Service** (email/SMS/webhooks) — uses event bus
* **Event Bus** — Kafka for async events (order.created → payment → delivery).
* **Cache** — Redis for hot reads (menus, geo queries).
* **Observability:** Prometheus metrics + Grafana dashboards; Zipkin/OpenTelemetry tracing; Loki for logs.
* **Resilience:** Resilience4j (circuit-breaker, bulkhead), retry policies, timeouts.
* **CI/CD:** GitHub Actions → build images → push to registry → deploy to k8s (or Docker Compose for demo).
* **Deployment targets:** Docker Compose for local demo; Kubernetes for production-ish demo (minikube/kind or a small cloud cluster).

---

## Service responsibilities & interaction (brief)

* **User Service**: auth registration, roles, basic profile. Exposes user events.
* **Restaurant Service**: menu versions, inventory snapshot, caching. Emits menu.updated.
* **Order Service**: orchestrates order flow; implements **Saga** pattern for distributed transactions (compensating actions). Emits order.* events.
* **Payment Service**: authorizes and captures (sandbox). Supports idempotency.
* **Delivery Service**: assigns riders, tracks location (WebSocket or polling stub).
* **Notification Service**: subscribes to events and sends emails/SMS/notifications asynchronously.

---

## Repo structure (monorepo — best for a showcase)

Use a mono-repo with clear modules so you can demonstrate cross-service CI and shared libs.

```
zenoeats/
├─ README.md
├─ infra/                   # docker-compose, k8s manifests, helm charts
├─ docs/                    # architecture.md, sequence diagrams, runbook
├─ shared/                  # common libs: dto, security, utils
├─ services/
│  ├─ user-service/
│  ├─ restaurant-service/
│  ├─ order-service/
│  ├─ payment-service/
│  ├─ delivery-service/
│  ├─ notification-service/
│  ├─ api-gateway/
│  └─ auth-service/
├─ scripts/                 # helper scripts for setup
└─ ci/                      # GitHub Actions workflows
```

Each `service`:

* Spring Boot 3 app (Java 21)
* Dockerfile
* Helm chart / k8s manifest stub
* README with run + API examples
* Integration tests folder

---

## Tech stack (concrete)

* Language: **Java 21**
* Framework: **Spring Boot 3**, Spring Cloud (Gateway, Config), Spring Security (JWT)
* Messaging: **Apache Kafka**
* Databases: **PostgreSQL** (service DBs) + optionally ClickHouse for analytics later
* Cache: **Redis**
* Observability: **OpenTelemetry/Zipkin**, **Prometheus**, **Grafana**, **Loki**
* Resilience: **Resilience4j**
* Container: **Docker**, local orchestration via **Docker Compose**, production demo via **Kubernetes** (kind/minikube)
* CI/CD: **GitHub Actions** → Docker Hub / GitHub Container Registry → k8s deployment
* Infra-as-code: basic **Helm charts** or simple k8s manifests

---

## Implementation roadmap — 8 weeks (5 hours/week minimum)

Timebox and deliver real artifacts each week. You will be asynchronous but accountable to deadlines.

### Week 1 — Foundations (deliverable: repo + README + skeleton services)

* Initialize mono-repo, basic folder structure.
* Create `user-service` and `api-gateway` skeletons with Dockerfiles.
* Add `README.md` with architecture summary and run instructions for Compose.
  **Outcome:** First commit showing intent and runnable demo for 2 services.

### Week 2 — Auth + User flows (deliverable: auth integrated, registration)

* Implement JWT-based auth in `auth-service` or integrate Keycloak stub.
* User registration/login flows and Postgres persistence.
* Basic API tests.
  **Outcome:** Authenticated user endpoints + documented API examples.

### Week 3 — Restaurant + Menu (deliverable: restaurant service + cache)

* Implement `restaurant-service` with menu CRUD, menu versioning, Redis caching.
* Add cache invalidation events.
  **Outcome:** Menu service with cache and tests.

### Week 4 — Order Service + Saga (deliverable: order lifecycle + events)

* Implement `order-service` with order state machine (created → paid → accepted → dispatched).
* Emit `order.created` to Kafka. Implement local saga orchestration (orchestrator pattern).
  **Outcome:** Order flows and event publishing.

### Week 5 — Payment + Idempotency (deliverable: payment service + integration)

* Implement `payment-service` (sandbox) with idempotency-key support.
* Simulate success/failure paths; implement compensating actions for failures.
  **Outcome:** End-to-end order → payment flow with compensation.

### Week 6 — Delivery + Notifications (deliverable: delivery assignment + notifications)

* Implement `delivery-service` stub (rider assignment logic).
* Notification service consumes events and sends emails (console/email stub).
  **Outcome:** Order -> payment -> delivery flow fully wired via Kafka events.

### Week 7 — Observability + Resilience (deliverable: tracing, metrics, circuit-breaker)

* Add Resilience4j patterns (retry, circuit-breaker).
* Integrate tracing (OpenTelemetry / Zipkin), Prometheus metrics scrape endpoints, Grafana quick dashboard.
  **Outcome:** Visible metrics and distributed traces for a sample purchase.

### Week 8 — CI/CD + Docs + Case Study (deliverable: GH Actions + architecture doc + blog post)

* Add CI pipeline: build, run unit tests, build docker images.
* Add k8s manifests or helm chart for demo deploy.
* Write a 1,200–1,500 word case study: "ZenoEats — architecture decisions, trade-offs, and lessons."
  **Outcome:** Deployable system demo + shareable case study for LinkedIn.

---

## Quality & hiring-focused artifacts (must have)

* **Architecture.md** — diagrams, flow charts, trade-offs (latency vs consistency, why Saga vs two-phase commit).
* **Runbook** — how to run locally and deploy to k8s.
* **Sequence diagrams** for critical flows (order payment refund).
* **One-page case study** with metrics (throughput simulated, failure handling).
* **LinkedIn posts**: short technical thread + link to repo/case study.

---

## Risk mitigation / trade-offs (what to defend in interviews)

* Using one DB per service vs. shared DB — justify data ownership and complexity.
* Saga pattern complexity — show compensating actions and observability to prove it works.
* Kafka for eventual consistency — measure and present latency/throughput expectations.
  Be ready to explain why each choice is the *right* trade-off for scalability and maintainability.
