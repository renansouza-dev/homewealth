# transactions

Spring Boot microservice that implements the **Transactions API** for the HomeWealth platform. It manages investment transactions (buy/sell operations) for a given portfolio, backed by PostgreSQL.

## Tech Stack

| Concern            | Library                                   |
|--------------------|-------------------------------------------|
| Framework          | Spring Boot + Spring MVC                  |
| Persistence        | Spring Data JPA + PostgreSQL              |
| Migrations         | Flyway                                    |
| API contract       | OpenAPI Generator (interface-only, Spring) |
| Mapping            | MapStruct                                 |
| Observability      | OpenTelemetry (traces, metrics, logs)     |
| Testing            | JUnit 5, MockMvc, Testcontainers, REST Assured |
| Coverage           | JaCoCo (minimum 85% line coverage)        |

## Getting Started

### Prerequisites

- Java 25+
- Maven (or use the included `./mvnw` wrapper)
- Docker (for local dependencies via Docker Compose)

### Running locally

Spring Boot Docker Compose support is included — starting the app will automatically bring up Postgres and Grafana:

```bash
./mvnw spring-boot:run -Dserver.port=8081
```

Or run explicitly with Docker Compose first:

```bash
docker compose up -d
./mvnw spring-boot:run
```

The API is available at: `http://localhost:8081/api/v1`

### Running tests

```bash
# Unit tests
./mvnw test

# All tests including integration (requires Docker for Testcontainers)
./mvnw verify
```

Coverage report is generated at `target/site/jacoco/index.html`.

## API Endpoints

All endpoints require a Bearer JWT token.

| Method   | Path                            | Description                               |
|----------|---------------------------------|-------------------------------------------|
| `POST`   | `/api/v1/transactions`          | Create a new transaction                  |
| `GET`    | `/api/v1/transactions`          | List transactions with filters and paging |
| `DELETE` | `/api/v1/transactions/{id}`     | Delete a transaction by UUID              |

Query parameters for `GET`: `portfolioId`, `assetId`, `fromDate`, `toDate`, `operationType`, `page`, `size`, `sortBy`, `sortOrder`.

For the full OpenAPI specification see [`src/main/resources/openapi/transactions-api-bundle.yaml`](src/main/resources/openapi/transactions-api-bundle.yaml).

## Configuration

Key settings in `application.yaml`:

| Property                          | Default                                    |
|-----------------------------------|--------------------------------------------|
| `server.port`                     | `8081`                                     |
| `spring.datasource.url`           | `jdbc:postgresql://localhost:5432/transactions` |
| `management.tracing.sampling.probability` | `1.0` (100% — development only)  |
| OTLP endpoint (metrics/traces/logs) | `http://localhost:4318`                  |

## Local Observability

The `compose.yaml` starts a [Grafana LGTM](https://hub.docker.com/r/grafana/otel-lgtm) stack (Loki + Grafana + Tempo + Mimir) alongside Postgres:

- Grafana UI: `http://localhost:3000`
- OTLP HTTP: `http://localhost:4318`

## How It Fits in the Monorepo

```
homewealth/
├── specifications/   ← API contract (OpenAPI specs)
└── transactions/     ← you are here (Spring Boot implementation)
```

The `specifications` module must be built first so its bundled YAML is available:

```bash
# From monorepo root
./mvnw install -pl specifications
./mvnw spring-boot:run -pl transactions
```