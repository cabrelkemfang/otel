# Employee Platform

A multi-module Spring Boot 4 REST API for employee management with built-in OpenTelemetry observability, caching, and structured error handling.

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Docker](#docker)
- [API Reference](#api-reference)
- [Data Model](#data-model)
- [Caching](#caching)
- [Observability](#observability)
- [Error Handling](#error-handling)
- [Project Structure](#project-structure)

## Architecture

The project follows a multi-module Maven layout with clear separation of concerns:

| Module | Purpose |
|--------|---------|
| **employee-service** | Domain logic — entities, DTOs, controllers, services, repositories, and mappers |
| **employee-launcher** | Runtime concerns — Boot entrypoint, configuration, Flyway migrations, exception translation, filters, and logging |

The launcher module depends on the service module and produces the runnable Spring Boot JAR (`otel-employee.jar`).

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Persistence | Spring Data JPA, Hibernate (validate mode) |
| Database | MySQL 8+ |
| Migrations | Flyway |
| Mapping | MapStruct 1.6.3 |
| Caching | Spring Cache + Caffeine |
| Validation | Jakarta Bean Validation |
| Observability | OpenTelemetry (traces, metrics, logs via OTLP) |
| Logging | Logback with async appenders + OTEL appender |
| Build | Maven Wrapper |

## Prerequisites

- **Java 21** or later
- **MySQL** running on `localhost:3306`
- **OTLP backend** (optional) — see [Observability](#observability) for the local Grafana LGTM stack

### Database Setup

The `application-local` profile creates the database automatically via the JDBC URL parameter. For other environments, create it manually:

```sql
CREATE DATABASE otel_employee_db;
```

Flyway will handle schema creation automatically on startup.

## Getting Started

### Build

```bash
./mvnw -q -DskipTests compile
```

### Run Tests

```bash
./mvnw -q test
```

### Package

```bash
./mvnw -q -DskipTests package
```

### Run Locally

The `local` profile provides pre-configured values for the datasource and OTLP endpoints (pointing to the LGTM stack on port `4320`):

```bash
java -Dspring.profiles.active=local \
  -jar employee-launcher/target/otel-employee.jar
```

> **Note:** Do not use `spring-boot:run` from the root — the root module is an aggregator POM. Always build from the root and run the launcher JAR directly.

### Run with Environment Variables

The default `application.yaml` requires the following environment variables:

| Variable | Example |
|----------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/otel_employee_db?...` |
| `SPRING_DATASOURCE_USERNAME` | `root` |
| `SPRING_DATASOURCE_PASSWORD` | `mysql` |
| `MANAGEMENT_OTLP_METRICS_EXPORT_URL` | `http://localhost:4320/v1/metrics` |
| `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT` | `http://localhost:4320/v1/traces` |
| `MANAGEMENT_OPENTELEMETRY_LOGGING_EXPORT_OTLP_ENDPOINT` | `http://localhost:4320/v1/logs` |

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/otel_employee_db \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=mysql \
java -jar employee-launcher/target/otel-employee.jar
```

## Docker

Two Docker Compose files are provided, designed to be started together.

### 1. Start the Grafana LGTM observability stack

```bash
docker compose -f otel-docker-compose.yml up -d
```

This starts the `grafana/otel-lgtm` all-in-one image (Loki + Grafana + Tempo + Mimir):

| Service | URL |
|---------|-----|
| Grafana UI | http://localhost:3001 (admin / admin) |
| OTLP gRPC | `localhost:4317` |
| OTLP HTTP | `localhost:4320` (mapped from internal `4318`) |

### 2. Start MySQL and the application

```bash
docker compose up -d
```

This starts MySQL and the Spring Boot application. The app connects to MySQL and sends telemetry to the LGTM stack over the shared `otel-net` network.

> The `otel-net` network is created by `otel-docker-compose.yml`, so start that first.

## API Reference

**Base path:** `/api/employees`

### Create Employee

```
POST /api/employees
```

**Request body:**

```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane.doe@example.com",
  "phoneNumber": "+1234567890",
  "department": "IT"
}
```

**Response:** `201 Created` with `EmployeeResponse`

### Get Employee by ID

```
GET /api/employees/{id}
```

**Response:** `200 OK` with `EmployeeResponse`

### List Employees (Paginated)

```
GET /api/employees?page=0&size=10&sortBy=createdAt&direction=asc
```

| Parameter | Default | Constraints |
|-----------|---------|-------------|
| `page` | `0` | min 0 |
| `size` | `10` | min 1, max 100 |
| `sortBy` | `createdAt` | any entity field |
| `direction` | `asc` | `asc` or `desc` |

**Response:** `200 OK` with `PaginatedResponse<EmployeeResponse>`

```json
{
  "response": [ ... ],
  "totalElements": 42,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 10
}
```

### Update Employee

```
PUT /api/employees/{id}
```

**Request body:** same as Create

**Response:** `200 OK` with `EmployeeResponse`

### Delete Employee

```
DELETE /api/employees/{id}
```

**Response:** `204 No Content`

### Response Format

```json
{
  "employeeId": 1,
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane.doe@example.com",
  "phoneNumber": "+1234567890",
  "department": "IT",
  "createdAt": "2026-04-02T10:30:00"
}
```

### Department Values

`HR` · `IT` · `FINANCE` · `OPERATIONS` · `MARKETING` · `LEGAL`

## Data Model

### Employee Table (`t_employee`)

| Column | Type | Constraints |
|--------|------|-------------|
| `employee_id` | BIGINT | PK, auto-increment |
| `first_name` | VARCHAR(100) | NOT NULL |
| `last_name` | VARCHAR(100) | NOT NULL |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE |
| `phone_number` | VARCHAR(20) | NOT NULL |
| `department` | VARCHAR(50) | NOT NULL |
| `created_at` | DATETIME | NOT NULL |

Schema is managed by Flyway migrations in `employee-platform-launcher/src/main/resources/db/migration/`.

## Caching

Employee lookups by ID are cached using Caffeine:

- **Cache name:** `employeeById`
- **Max entries:** 1,000
- **TTL:** 10 minutes (expire after write)
- Cache is updated on `PUT` and evicted on `DELETE`

## Observability

### OpenTelemetry

The application exports all three telemetry signals via OTLP. Endpoints are configured via environment variables (see [Getting Started](#getting-started)):

| Signal | Property |
|--------|----------|
| Traces | `management.opentelemetry.tracing.export.otlp.endpoint` |
| Metrics | `management.otlp.metrics.export.url` |
| Logs | `management.opentelemetry.logging.export.otlp.endpoint` |

In the local profile all three point to the Grafana LGTM stack at `http://localhost:4320/v1/{signal}`.

Tracing is configured with **100% sampling** by default (suitable for development; lower this in production).

### Logging

Log output includes trace context fields (`traceId`, `spanId`) automatically when Micrometer Tracing is active.

Appenders:
- **Console** — async, 512-entry queue
- **File** — async rolling file at `logs/` (10 MB rotation, 14-day retention, 1 GB cap)
- **OTEL** — forwards logs to the OpenTelemetry-compatible backend via the `OpenTelemetryAppender`

## Error Handling

All errors are returned as RFC 9457 `ProblemDetail` responses:

| Scenario | Status | Example Detail |
|----------|--------|----------------|
| Employee not found | `404` | Employee not found with id: 42 |
| Duplicate email | `409` | An employee with email 'x@y.com' already exists |
| Validation failure | `400` | Validation failed (includes field-level error map) |

## Project Structure

```
otel-employee/
├── pom.xml                              # Aggregator POM
├── docker-compose.yml                   # MySQL + application stack
├── otel-docker-compose.yml              # Grafana LGTM observability stack
├── Dockerfile                           # Application image
├── employee-service/                    # Domain & API module
│   ├── pom.xml
│   └── src/main/java/.../employee/
│       ├── configuration/               # JPA, component scan config
│       ├── controller/                  # REST controllers
│       ├── dto/                         # Request/response DTOs
│       ├── entity/                      # JPA entities
│       ├── exception/                   # Domain exceptions
│       ├── mapper/                      # MapStruct mappers
│       ├── repository/                  # Spring Data repositories
│       └── service/                     # Service interfaces & impls
├── employee-launcher/                   # Runtime module
│   ├── pom.xml
│   └── src/main/
│       ├── java/.../launcher/
│       │   ├── EmployeeApplication.java # Boot entrypoint
│       │   ├── configuration/           # Runtime configuration beans
│       │   ├── exception/               # Global exception handler (ProblemDetail)
│       │   └── filter/                  # Servlet filters (trace body capture)
│       └── resources/
│           ├── application.yaml         # Base config (env-var driven)
│           ├── application-local.yaml   # Local development overrides
│           ├── application-test.yaml    # Test overrides
│           ├── logback-spring.xml       # Logging config (console + file + OTEL)
│           └── db/migration/            # Flyway SQL migrations
└── logs/                                # Runtime log output
```
