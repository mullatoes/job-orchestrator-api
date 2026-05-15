# Job Orchestrator API

A production-style background job orchestrator built on Spring Boot 4 and PostgreSQL. The API accepts work items over HTTP, persists them durably, and processes them asynchronously through a polling worker that supports retries, dead-lettering, stale-job recovery, and horizontal scale-out via row-level locking.

This repo is a learning sandbox for building the kind of resilient, observable, distributed background-processing system you would expect to find behind any non-trivial product surface — payments, notifications, ETL, async exports — without reaching for a dedicated broker (Kafka, RabbitMQ, SQS). PostgreSQL is the queue.

---

## What it does

Clients submit a `Job` describing some unit of work (e.g. `EMAIL_NOTIFICATION`, `REPORT_GENERATION`). The orchestrator:

1. Persists the job atomically and assigns a public `jobId` and `correlationId`.
2. A background worker polls the DB on a fixed cadence, **claims** a batch of ready jobs using `SELECT ... FOR UPDATE SKIP LOCKED`, and executes them in isolated transactions.
3. Failures are retried with a delay up to `maxAttempts`. Exhausted jobs land in `DEAD_LETTER`.
4. A separate recovery loop reclaims jobs that have been stuck in `PROCESSING` past a stale-timeout (e.g. a worker crashed mid-execution).
5. Operators can resurrect a dead-lettered job via a dedicated retry endpoint.

Every step is traceable end-to-end via a `X-Correlation-Id` that flows from the HTTP request, into the persisted row, and back out into every worker log line that touches the job.

---

## Architecture

```
┌──────────┐   POST /api/v1/jobs    ┌──────────────────┐
│  Client  │ ─────────────────────▶ │  JobController   │
└──────────┘                        └────────┬─────────┘
                                             │
                                             ▼
                                    ┌──────────────────┐
                                    │   JobService     │  ──▶ PENDING row in `jobs`
                                    └──────────────────┘
                                             │
                                             │  (decoupled by DB state)
                                             ▼
        ┌─────────────────── JobScheduler (@Scheduled) ───────────────────┐
        │                                                                 │
        ▼                                                                 ▼
┌──────────────────┐                                          ┌──────────────────────┐
│ JobWorkerService │  claim ──▶ execute ──▶ complete/retry    │ JobRecoveryService   │
│                  │                                          │ (rescue stale rows)  │
└────────┬─────────┘                                          └──────────┬───────────┘
         │                                                               │
         ▼                                                               ▼
┌──────────────────┐   FOR UPDATE SKIP LOCKED   ┌──────────────────────────────────┐
│ JobClaimService  │ ─────────────────────────▶ │             PostgreSQL           │
│ JobExecutionSvc  │                            │     jobs table (Flyway-managed)  │
└──────────────────┘                            └──────────────────────────────────┘
```

### Key modules

| Package | Responsibility |
| --- | --- |
| [controller](src/main/java/com/jdevs/joborchestratorapi/controller) | REST surface (`/api/v1/jobs`). |
| [service/JobService.java](src/main/java/com/jdevs/joborchestratorapi/service/JobService.java) | Submit, look up, search, manual retry of dead-lettered jobs. |
| [service/JobClaimService.java](src/main/java/com/jdevs/joborchestratorapi/service/JobClaimService.java) | Atomically claims a batch using `SELECT ... FOR UPDATE SKIP LOCKED`. |
| [service/JobExecutionService.java](src/main/java/com/jdevs/joborchestratorapi/service/JobExecutionService.java) | Runs each claimed job in its own `REQUIRES_NEW` transaction; resolves to `COMPLETED`, `RETRYABLE`, or `DEAD_LETTER`. |
| [service/JobRecoveryService.java](src/main/java/com/jdevs/joborchestratorapi/service/JobRecoveryService.java) | Periodically rescues rows stuck in `PROCESSING` past `stale-timeout-seconds`. |
| [service/JobProcessor.java](src/main/java/com/jdevs/joborchestratorapi/service/JobProcessor.java) | Pluggable business logic dispatched on `JobType`. Honors a `forceFail=true` payload flag for retry testing. |
| [scheduler/JobScheduler.java](src/main/java/com/jdevs/joborchestratorapi/scheduler/JobScheduler.java) | Two `@Scheduled` loops: worker cycle + recovery cycle. |
| [filter/RequestCorrelationFilter.java](src/main/java/com/jdevs/joborchestratorapi/filter/RequestCorrelationFilter.java) | Reads/generates `X-Correlation-Id`, binds it to MDC, echoes it back on the response. |
| [exception/GlobalExceptionHandler.java](src/main/java/com/jdevs/joborchestratorapi/exception/GlobalExceptionHandler.java) | Maps domain & validation errors to a uniform `ApiResponse` envelope. |

### Job lifecycle

```
                  submit
                    │
                    ▼
                ┌─────────┐  claim batch  ┌────────────┐
                │ PENDING │ ────────────▶ │ PROCESSING │
                └─────────┘               └─────┬──────┘
                                                │
                  ┌─────────────────────────────┼────────────────────────────┐
                  │                             │                            │
              success                  attempt < max                  attempt >= max
                  │                             │                            │
                  ▼                             ▼                            ▼
            ┌───────────┐                ┌───────────┐                ┌─────────────┐
            │ COMPLETED │                │ RETRYABLE │ ─ delay ─▶ claim ─ │ DEAD_LETTER │
            └───────────┘                └───────────┘                └──────┬──────┘
                                                                            │
                                                                  manual retry
                                                                            │
                                                                            ▼
                                                                      RETRYABLE
```

Recovery: any job that has been `PROCESSING` for longer than `stale-timeout-seconds` is treated as if its worker died, bumping its attempt count and routing it to `RETRYABLE` or `DEAD_LETTER` accordingly.

---

## Tech stack

- **Java 21**, **Spring Boot 4.0.6** (`spring-boot-starter-webmvc`, `data-jpa`, `validation`, `actuator`).
- **PostgreSQL 16** as the durable queue.
- **Flyway** for versioned schema migrations (`db/migration`).
- **Hibernate / JPA** with `@Version` for optimistic locking and `PESSIMISTIC_WRITE` via native `FOR UPDATE SKIP LOCKED`.
- **Lombok** for boilerplate reduction.
- **SLF4J + MDC** for structured, correlation-aware logging.
- **Spring `@Scheduled`** for the worker and recovery loops.
- **Docker** (multi-stage build) + **Docker Compose** for local containerized runs.
- **Kubernetes** manifests (Deployment, Service, ConfigMap, Secret) targeted at Minikube but cluster-agnostic.

---

## Concepts this repo demonstrates

This is intentionally not a "Hello World." The code is a deliberate study of patterns you would expect on a senior-level checklist:

- **PostgreSQL as a job queue** using `FOR UPDATE SKIP LOCKED` — multiple worker pods can poll the same table without stomping each other.
- **Optimistic + pessimistic locking** working together: row-level locks for claim, `@Version` for state transitions.
- **At-least-once semantics with bounded retries** and an explicit dead-letter sink (no silent loss).
- **Stale-job recovery** so a crashed worker does not leave permanently-stuck rows.
- **Idempotent worker design** — `JobExecutionService` re-checks `isProcessable(job)` before doing work.
- **Per-job transaction isolation** via `Propagation.REQUIRES_NEW` so one bad job cannot poison a whole batch.
- **End-to-end correlation IDs** propagated from HTTP request → DB row → background log lines.
- **Structured logging** with a custom Logback pattern that includes `correlationId`, `jobId`, `workerId`.
- **Flyway-managed schema evolution** (two migrations so far — see `V1__create_jobs_table.sql` and `V2__add_correlation_id_to_jobs.sql`).
- **Spring Actuator** with separate `liveness` and `readiness` probes wired into Kubernetes.
- **Configuration as typed properties** via `@ConfigurationProperties` ([JobWorkerProperties](src/main/java/com/jdevs/joborchestratorapi/config/JobWorkerProperties.java)).
- **Uniform API envelope** + global exception handler that maps validation, not-found, and invalid-state errors to consistent HTTP codes.
- **Pagination** at the controller boundary with `Pageable` and a custom `PagedResponse` DTO.
- **12-factor configuration** — every infra knob is overridable via environment variables.

---

## API reference

Base path: `http://localhost:8080/api/v1/jobs`

All responses use the envelope:

```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "timestamp": "2026-05-15T10:15:30.123"
}
```

### Submit a job — `POST /api/v1/jobs`

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: 7f3c2a98-2c5e-4b9b-9d27-12b1d2c1f1aa' \
  -d '{
    "jobType": "EMAIL_NOTIFICATION",
    "maxAttempts": 3,
    "payload": {
      "to": "alice@example.com",
      "subject": "Welcome",
      "templateId": "welcome-v2"
    }
  }'
```

**201 Created**

```json
{
  "success": true,
  "message": "Job submitted successfully",
  "data": {
    "jobId": "JOB-3F9A12C4",
    "correlationId": "7f3c2a98-2c5e-4b9b-9d27-12b1d2c1f1aa",
    "jobType": "EMAIL_NOTIFICATION",
    "status": "PENDING"
  },
  "timestamp": "2026-05-15T10:15:30.123"
}
```

Supported `jobType` values: `EMAIL_NOTIFICATION`, `SMS_NOTIFICATION`, `REPORT_GENERATION`, `DATA_SYNC`. `maxAttempts` is optional (default `3`, range `1..10`).

> Tip: include `"forceFail": true` in the payload to trigger the deliberate failure path and watch the retry / dead-letter machinery work end-to-end.

### Fetch a single job — `GET /api/v1/jobs/{jobId}`

```bash
curl http://localhost:8080/api/v1/jobs/JOB-3F9A12C4
```

### Search jobs — `GET /api/v1/jobs?status=...&page=...&size=...`

```bash
# All jobs, page 0, default size 10
curl 'http://localhost:8080/api/v1/jobs'

# Only failed-but-retryable jobs, second page of 25
curl 'http://localhost:8080/api/v1/jobs?status=RETRYABLE&page=1&size=25'
```

`status` is optional and accepts any value of `JobStatus`: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `RETRYABLE`, `DEAD_LETTER`. `page` must be `>= 0`, `size` must be `1..100`.

### Manually retry a dead-lettered job — `POST /api/v1/jobs/{jobId}/retry`

```bash
curl -X POST http://localhost:8080/api/v1/jobs/JOB-3F9A12C4/retry
```

**200 OK**

```json
{
  "success": true,
  "message": "Job requeued successfully",
  "data": {
    "jobId": "JOB-3F9A12C4",
    "previousStatus": "DEAD_LETTER",
    "currentStatus": "RETRYABLE",
    "attemptCount": 0
  },
  "timestamp": "2026-05-15T10:20:00.000"
}
```

Returns `400 Bad Request` if the job is not currently in `DEAD_LETTER`.

### Operational endpoints

| Endpoint | Purpose |
| --- | --- |
| `GET /actuator/health` | Aggregate health. |
| `GET /actuator/health/liveness` | Kubernetes liveness probe target. |
| `GET /actuator/health/readiness` | Kubernetes readiness probe target. |
| `GET /actuator/info` | Build / app info. |

---

## Configuration

All knobs live in [application.yaml](src/main/resources/application.yaml) and the `job-worker.*` block is bound to [JobWorkerProperties](src/main/java/com/jdevs/joborchestratorapi/config/JobWorkerProperties.java).

| Property | Env override | Default | Meaning |
| --- | --- | --- | --- |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5434/job_orchestrator_db` | DB connection. |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `job_user` | DB user. |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `job_password` | DB password. |
| `job-worker.enabled` | — | `true` | Master kill-switch for the worker + recovery loops. |
| `job-worker.batch-size` | — | `5` | Max jobs claimed per worker tick. |
| `job-worker.fixed-delay-ms` | — | `10000` | Polling cadence of the worker loop. |
| `job-worker.retry-delay-seconds` | — | `15` | Delay applied to `RETRYABLE` jobs before they become eligible again. |
| `job-worker.worker-id` | `JOB_WORKER_ID` | `local-worker-1` | Identifies this worker instance in logs and `locked_by`. |
| `job-worker.stale-timeout-seconds` | — | `60` | A `PROCESSING` job older than this is considered abandoned. |
| `job-worker.recovery-fixed-delay-ms` | — | `30000` | Cadence of the stale-job recovery loop. |

---

## Running it

### Prerequisites

- JDK 21
- Docker / Docker Compose
- (Optional, for k8s path) Minikube + `kubectl`

### Option 1 — Local JVM against a containerized Postgres

```bash
# Start Postgres only (exposes 5434 → 5432)
docker compose up -d postgres

# Run the app on the host
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Flyway runs migrations on boot. Submit a job and watch the `Claimed jobs for processing` log line tick by within ~10s.

### Option 2 — Full Docker Compose

```bash
docker compose up --build
```

This brings up `postgres` and `job-orchestrator-api` together. Postgres is healthchecked, so the API will wait for it before starting.

### Option 3 — Minikube / Kubernetes

The `k8s/` directory contains everything: Postgres Deployment + Service + Secret, and the API's Deployment + Service + ConfigMap + Secret.

```bash
# 1. Start Minikube
minikube start

# 2. Point your local Docker CLI at Minikube's Docker daemon so the built
#    image is visible inside the cluster without a registry push.
eval $(minikube docker-env)

# 3. Build the image inside Minikube
docker build -t job-orchestrator-api:1.0.0 .

# 4. Apply manifests (order matters: secrets/configmaps first)
kubectl apply -f k8s/postgres-secret.yml
kubectl apply -f k8s/postgres-deployment.yml
kubectl apply -f k8s/postgres-service.yml

kubectl apply -f k8s/app-secret.yml
kubectl apply -f k8s/app-configmap.yml
kubectl apply -f k8s/app-deployment.yml
kubectl apply -f k8s/app-service.yml

# 5. Watch pods come up
kubectl get pods -w

# 6. Expose the NodePort service
minikube service job-orchestrator-api-service --url
# → use the returned URL as the base for the curl examples above
```

To scale horizontally and exercise the `SKIP LOCKED` claim path:

```bash
kubectl scale deployment job-orchestrator-api --replicas=3
kubectl logs -l app=job-orchestrator-api -f --max-log-requests=10
```

Each replica claims a disjoint batch on every tick; `locked_by` in the `jobs` table will show different `workerId` values.

To tear it all down:

```bash
kubectl delete -f k8s/
```

> **Production caveat.** The secrets in `k8s/*-secret.yml` are committed in plaintext on purpose — this is a learning repo. In any real environment route them through Sealed Secrets / External Secrets / a vault.

---

## Project layout

```
.
├── Dockerfile                       # Multi-stage build (temurin:21-jdk → temurin:21-jre)
├── docker-compose.yml               # Postgres + API
├── k8s/                             # Kubernetes manifests (Minikube-friendly)
├── pom.xml                          # Spring Boot 4 / Java 21
└── src
    ├── main
    │   ├── java/com/jdevs/joborchestratorapi
    │   │   ├── config/              # @ConfigurationProperties
    │   │   ├── controller/          # REST controllers
    │   │   ├── dto/                 # Request / response DTOs + ApiResponse envelope
    │   │   ├── entity/              # JPA entities
    │   │   ├── enums/               # JobStatus, JobType
    │   │   ├── exception/           # Domain exceptions + @RestControllerAdvice
    │   │   ├── filter/              # Correlation-ID servlet filter
    │   │   ├── logging/             # MDC key constants
    │   │   ├── repository/          # Spring Data JPA + native claim query
    │   │   ├── scheduler/           # @Scheduled worker + recovery loops
    │   │   └── service/             # Submit / claim / execute / recover / process
    │   └── resources
    │       ├── application.yaml
    │       └── db/migration         # Flyway: V1, V2
    └── test
        └── java/com/jdevs/joborchestratorapi
```

---

## What I'm learning here

This project is a deliberate walk through the design space of a resilient, observable, distributed background-processing service. Concretely:

- How to use **Postgres as a queue** correctly — `FOR UPDATE SKIP LOCKED`, batch sizing, and indexing the right columns (`status`, `status + next_retry_at`).
- How to **partition concerns** between claim, execute, and recover, and why each needs its own transaction boundary.
- How to **scale workers horizontally** without coordination beyond the database.
- How to **survive worker crashes** with a stale-recovery loop instead of relying on broker-side ack semantics.
- How to design **at-least-once retry pipelines** with explicit dead-lettering and a human-in-the-loop retry path.
- How to **trace a single logical operation** through a synchronous HTTP request *and* the asynchronous background execution that follows.
- How to **ship the same artifact** to a laptop, to Compose, and to Kubernetes with no code changes — only environment-level configuration.
- How modern Spring Boot 4 + Java 21 idioms (records, `var`, text blocks, `@ConfigurationProperties`) feel in a realistic codebase.
