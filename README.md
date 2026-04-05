# Employee Platform

A multi-module Spring Boot 4 REST API for employee management with built-in OpenTelemetry observability, caching, and structured error handling.

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Data Model](#data-model)
- [Seed Data](#seed-data)
- [Caching](#caching)
- [Observability](#observability)
- [Error Handling](#error-handling)
- [Project Structure](#project-structure)

## Architecture

The project follows a multi-module Maven layout with clear separation of concerns:

| Module | Purpose |
|--------|---------|
| **employee-platform** | Domain logic — entities, DTOs, controllers, services, repositories, and mappers |
| **employee-platform-launcher** | Runtime concerns — Boot entrypoint, configuration, Flyway migrations, exception translation, filters, and logging |

The launcher module depends on the platform module and produces the runnable Spring Boot JAR.

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
| Observability | OpenTelemetry API (traces, metrics, logs via OTLP) |
| Logging | Logback with OpenTelemetry appender |
| Build | Maven Wrapper |

## Prerequisites

- **Java 21** or later
- **MySQL** running on `localhost:3306`
- **OpenTelemetry Collector** (optional) listening on `localhost:4318` for traces, metrics, and logs

### Database Setup

The Flyway V1 migration creates the database and schema automatically:

```sql
CREATE DATABASE IF NOT EXISTS otel_employee_db;
```

The JDBC URL is also configured with `createDatabaseIfNotExist=true`, so no manual setup is required.

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

### Run

```bash
java -jar employee-platform-launcher/target/employee-platform-launcher-0.0.1-SNAPSHOT.jar
```

> **Note:** Do not use `spring-boot:run` from the root — the root module is an aggregator POM. Always build from the root and run the launcher JAR directly.

### Configuration Overrides

Override defaults via environment variables or system properties:

```bash
java -jar employee-platform-launcher/target/employee-platform-launcher-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://host:3306/mydb \
  --spring.datasource.username=myuser \
  --spring.datasource.password=mypass
```

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

### Migrations

| Version | File | Description |
|---------|------|-------------|
| V1 | `V1__create_employee_table.sql` | Creates the `otel_employee_db` database and `t_employee` table |
| V2 | `V2__insert_sample_employees.sql` | Seeds three sample employees |

## Seed Data

The V2 migration inserts sample employees so the API is usable immediately after startup:

| Name | Email | Department |
|------|-------|------------|
| Alice Johnson | alice.johnson@growtogether.io | FINANCE |
| Bob Smith | bob.smith@growtogether.io | OPERATIONS |
| Carol Williams | carol.williams@growtogether.io | HR |

## Caching

Employee lookups by ID are cached using Caffeine:

- **Cache name:** `employeeById`
- **Max entries:** 1,000
- **TTL:** 10 minutes (expire after write)
- Cache is updated on `PUT` and evicted on `DELETE`

## Observability

### OpenTelemetry

The application exports telemetry data via OTLP to `localhost:4318`:

| Signal | Endpoint |
|--------|----------|
| Traces | `http://localhost:4318/v1/traces` |
| Metrics | `http://localhost:4318/v1/metrics` |
| Logs | `http://localhost:4318/v1/logs` |

Tracing is configured with **100% sampling** by default.

### Trace Body Capture

The `ProductionTraceBodyFilter` uses the **OpenTelemetry API directly** (`io.opentelemetry.api.trace.Span`) to attach request and response bodies as span attributes, making them searchable in **Grafana Tempo**.

| Span Attribute | Description |
|----------------|-------------|
| `http.request.body` | Sanitized and truncated request body |
| `http.response.body` | Sanitized and truncated response body |
| `http.response.body.size` | Raw response body size in bytes |

**Safety features:**

- **Sensitive data masking** — JSON values for keys `password`, `token`, `secret`, and `authorization` are replaced with `******`
- **Truncation** — Bodies larger than 2 KB are truncated to avoid excessive Tempo storage costs
- **Error resilience** — Capture failures are recorded as `*.error` attributes instead of propagating exceptions

### Trace ID Header

The same filter sets the `X-Trace-Id` response header on every request, allowing clients to correlate responses with traces in Tempo.

### Logging

Log output is sent to two appenders:

| Appender | Description |
|----------|-------------|
| **CONSOLE** | Spring Boot default console output (via `base.xml` include) |
| **OTEL** | Forwards structured logs to the OpenTelemetry Collector with MDC, code attributes, and markers |

Log output includes trace context automatically via Spring Boot's default log pattern.

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
├── employee-platform/                   # Domain & API module
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
├── employee-platform-launcher/          # Runtime module
│   ├── pom.xml
│   └── src/main/
│       ├── java/.../launcher/
│       │   ├── EmployeeApplication.java # Boot entrypoint
│       │   ├── exception/               # Global exception handler
│       │   └── filter/                  # Tracing filter (body capture + trace ID header)
│       └── resources/
│           ├── application.yaml         # Application config
│           ├── logback-spring.xml       # Logging config
│           └── db/migration/            # Flyway SQL migrations
│               ├── V1__create_employee_table.sql
│               └── V2__insert_sample_employees.sql
└── logs/                                # Runtime log output
```
