# oracle-query-sentinel

> Real-time Oracle SQL performance monitoring and anomaly detection — built for high-throughput scientific data environments.

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=java)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![Oracle](https://img.shields.io/badge/Oracle-XE_21c-F80000?style=flat-square&logo=oracle)](https://www.oracle.com/database/)
[![Docker](https://img.shields.io/badge/Docker-ready-2496ED?style=flat-square&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)](LICENSE)

---

## Why this exists

Large-scale scientific systems — particle accelerators, telescope arrays, sensor grids — generate millions of database writes per second. A single unoptimized query can cascade into systemic latency under that kind of load.

`oracle-query-sentinel` continuously interrogates Oracle's internal performance views (`V$SQL`, `V$SESSION`, `V$LOCK`, AWR) to surface slow queries, deadlocks, and missing indexes *before* they become incidents. Think of it as a smoke detector for your database layer.

---

## Features

- **Full table scan detection** — flags queries performing sequential reads on large tables
- **Slow query tracking** — configurable elapsed-time thresholds per schema
- **Deadlock & lock contention alerts** — real-time detection via `V$LOCK` and `V$SESSION`
- **Explain Plan analysis** — automatic query plan capture for flagged statements
- **N+1 query detection** — identifies repeated identical queries within a session window
- **REST API** — query anomalies programmatically, export as JSON or CSV
- **Configurable rules** — all thresholds defined in `application.yml`, no recompile needed
- **CI-ready** — Oracle XE in Docker for reproducible integration tests

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Spring Batch Jobs                  │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │ V$SQL reader│  │ V$LOCK reader│  │ AWR reader│  │
│  └──────┬──────┘  └──────┬───────┘  └─────┬─────┘  │
└─────────┼────────────────┼────────────────┼─────────┘
          │                │                │
          ▼                ▼                ▼
┌─────────────────────────────────────────────────────┐
│              Anomaly Detection Engine               │
│  Rules loaded from YAML — pluggable detector chain  │
└─────────────────────────┬───────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
   ┌────────────┐  ┌────────────┐  ┌──────────────┐
   │ REST API   │  │ CSV Export │  │ Alert Logger │
   │ (OpenAPI 3)│  │            │  │ (SLF4J/JSON) │
   └────────────┘  └────────────┘  └──────────────┘
```

---

## Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 17 | Records, sealed classes, pattern matching |
| Framework | Spring Boot 3.2 | Production-grade, battle-tested at scale |
| Batch | Spring Batch | Reliable polling with retry/skip semantics |
| Database | Oracle XE 21c (JDBC) | Direct access to internal perf views |
| Testing | JUnit 5 + Testcontainers | Integration tests against real Oracle instance |
| API Docs | SpringDoc / OpenAPI 3 | Auto-generated, always in sync |
| Build | Maven | Reproducible builds, CI-friendly |
| Infra | Docker Compose | One-command local stack |

---

## Quick start

### Prerequisites

- Docker & Docker Compose
- Java 17+
- Maven 3.9+

### Run locally

```bash
git clone https://github.com/yourhandle/oracle-query-sentinel.git
cd oracle-query-sentinel

# Start Oracle XE + application
docker compose up -d

# Or run against an existing Oracle instance
cp src/main/resources/application.yml.example src/main/resources/application.yml
# Edit datasource.url / username / password
mvn spring-boot:run
```

The API is available at `http://localhost:8080` — Swagger UI at `/swagger-ui.html`.

---

## Configuration

All detection rules live in `application.yml`:

```yaml
sentinel:
  polling-interval-seconds: 30
  rules:
    slow-query-threshold-ms: 2000
    full-scan-min-rows: 10000
    lock-wait-threshold-seconds: 5
    n-plus-one-window-seconds: 10
    n-plus-one-min-occurrences: 5
  schemas:
    - HR
    - ATLAS_DATA
```

No recompile. No redeploy. Change the file, restart.

---

## API

```
GET  /api/v1/anomalies              → list detected anomalies (paginated)
GET  /api/v1/anomalies/{id}         → anomaly detail + explain plan
GET  /api/v1/anomalies/export       → CSV export
GET  /api/v1/sessions/locks         → current lock contention
GET  /api/v1/metrics/summary        → aggregated stats by schema
```

Example response:

```json
{
  "id": "anomaly-0042",
  "type": "FULL_TABLE_SCAN",
  "schema": "ATLAS_DATA",
  "sql_id": "9fz3hkqp2c1x8",
  "elapsed_ms": 14320,
  "rows_examined": 48200000,
  "detected_at": "2025-11-14T03:17:42Z",
  "explain_plan": "TABLE ACCESS FULL | EVENTS_RAW | cost=98312",
  "recommendation": "Consider index on EVENTS_RAW(timestamp, detector_id)"
}
```

---

## Testing

```bash
# Unit tests only
mvn test

# Full integration suite (starts Oracle XE via Testcontainers)
mvn verify -P integration-tests

# With coverage report
mvn verify jacoco:report
open target/site/jacoco/index.html
```

Integration tests spin up a real Oracle XE container, seed it with synthetic load data, and assert that all detectors fire correctly. No mocks for the Oracle layer.

---

## Project structure

```
src/
├── main/java/dev/sentinel/
│   ├── batch/          # Spring Batch jobs (readers, processors, writers)
│   ├── detector/       # Pluggable anomaly detector chain
│   ├── model/          # Domain model (Anomaly, Session, LockInfo…)
│   ├── repository/     # JDBC repositories querying V$ views
│   ├── api/            # REST controllers + OpenAPI annotations
│   └── config/         # Rule configuration binding
└── test/
    ├── unit/           # Pure logic tests
    └── integration/    # Testcontainers Oracle tests
```

---

## Roadmap

- [ ] Grafana dashboard via Prometheus metrics endpoint
- [ ] Email / Slack alerting for critical anomalies
- [ ] Historical trend analysis (anomaly frequency over time)
- [ ] Multi-database support (PostgreSQL adapter)
- [ ] gRPC streaming endpoint for real-time consumers

---

## Context

Built as a portfolio project to demonstrate production-grade Java/Oracle backend development — the kind of database instrumentation work that matters in high-data-volume environments like scientific computing infrastructure.

The Oracle internal views used here (`V$SQL`, `V$SESSION`, `V$LOCK`, `DBA_HIST_SQLSTAT`) are the same ones Oracle DBAs use in production diagnostics. This isn't a toy app querying a demo schema — it's instrumentation of the database engine itself.

---

## License

MIT — see [LICENSE](LICENSE).
