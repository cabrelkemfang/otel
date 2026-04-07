# Copilot Instructions for otel-employee

## Purpose
Use these instructions when making changes in this repository. Keep changes minimal, preserve module boundaries, and follow existing Spring Boot patterns in code and configuration.

## Repository Shape
- Multi-module Maven project rooted at [pom.xml](pom.xml).
- Domain and API module: [employee-platform](employee-platform).
- Runnable application module: [employee-platform-launcher](employee-platform-launcher).

## Technology Stack
- **Java 21** / **Spring Boot 4.0.5** — see root [pom.xml](pom.xml).
- **MapStruct 1.6.3** for compile-time entity/DTO mapping.
- **Caffeine** for in-memory caching; cache names: `employeeById`.
- **Flyway + MySQL 8.0+** for schema management; JPA in `validate` mode (no auto DDL).
- **OpenTelemetry (OTLP/HTTP)** + **Grafana LGTM** for traces, metrics, and logs.

## Build, Test, and Run
- Build all modules from repository root:
  - `./mvnw -q -DskipTests compile`
- Run tests from repository root:
  - `./mvnw -q test`
- Build runnable artifact:
  - `./mvnw -q -DskipTests package`
- Run application jar:
  - `java -jar employee-platform-launcher/target/employee-platform-launcher-0.0.1-SNAPSHOT.jar`
- Start full observability stack (Grafana LGTM + OTel Collector):
  - `docker compose -f otel-docker-compose.yml up -d`
  - Grafana UI: http://localhost:3000 (admin/admin)
- Start everything including MySQL and the app:
  - `docker compose -f otel-docker-compose.yml -f docker-compose.yml up -d`

Notes:
- Java 21 is required (see [pom.xml](pom.xml)).
- Prefer root reactor commands over running Maven directly inside a child module.
- Avoid relying on root-level spring-boot:run, because the root module is an aggregator POM.

## Environment Assumptions
- MySQL expected at localhost:3306 and currently configured in [employee-platform-launcher/src/main/resources/application.yaml](employee-platform-launcher/src/main/resources/application.yaml).
- Flyway is enabled and migrations live under [employee-platform-launcher/src/main/resources/db/migration](employee-platform-launcher/src/main/resources/db/migration).
- OTLP endpoints are configured for localhost:4318 in [employee-platform-launcher/src/main/resources/application.yaml](employee-platform-launcher/src/main/resources/application.yaml).

## Architecture and Boundaries
- Keep domain and CRUD behavior in employee-platform:
  - Controller pattern: [employee-platform/src/main/java/io/growtogether/employee/controller/EmployeeController.java](employee-platform/src/main/java/io/growtogether/employee/controller/EmployeeController.java)
  - Service pattern: [employee-platform/src/main/java/io/growtogether/employee/service/impl/EmployeeServiceImpl.java](employee-platform/src/main/java/io/growtogether/employee/service/impl/EmployeeServiceImpl.java)
  - Mapping pattern: [employee-platform/src/main/java/io/growtogether/employee/mapper/EmployeeMapper.java](employee-platform/src/main/java/io/growtogether/employee/mapper/EmployeeMapper.java)
  - Auto-configuration export: [employee-platform/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](employee-platform/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
- Keep runtime and cross-cutting concerns in employee-platform-launcher:
  - Boot entrypoint: [employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/EmployeeApplication.java](employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/EmployeeApplication.java)
  - Global exception translation: [employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/exception/GlobalExceptionHandler.java](employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/exception/GlobalExceptionHandler.java)
  - Correlation and tracing filter: [employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/filter/CorrelationIdFilter.java](employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/filter/CorrelationIdFilter.java)

## Coding Conventions to Follow
- Use bean validation on request DTOs and controller parameters:
  - Request DTO example: [employee-platform/src/main/java/io/growtogether/employee/dto/EmployeeRequest.java](employee-platform/src/main/java/io/growtogether/employee/dto/EmployeeRequest.java)
  - Controller parameter validation example: [employee-platform/src/main/java/io/growtogether/employee/controller/EmployeeController.java](employee-platform/src/main/java/io/growtogether/employee/controller/EmployeeController.java)
- Service layer throws domain exceptions for not found and conflicts:
  - [employee-platform/src/main/java/io/growtogether/employee/exception/EmployeeNotFoundException.java](employee-platform/src/main/java/io/growtogether/employee/exception/EmployeeNotFoundException.java)
  - [employee-platform/src/main/java/io/growtogether/employee/exception/DuplicateEmailException.java](employee-platform/src/main/java/io/growtogether/employee/exception/DuplicateEmailException.java)
- API errors should be translated centrally with ProblemDetail in launcher advice:
  - [employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/exception/GlobalExceptionHandler.java](employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/exception/GlobalExceptionHandler.java)
- Use MapStruct for entity/DTO mapping and keep identity/audit fields ignored during create/update operations:
  - [employee-platform/src/main/java/io/growtogether/employee/mapper/EmployeeMapper.java](employee-platform/src/main/java/io/growtogether/employee/mapper/EmployeeMapper.java)
- Apply caching annotations on the service implementation—not the interface—using the `employeeById` cache:
  - `@Cacheable(cacheNames="employeeById", key="#id")` on `findById()`
  - `@CachePut(cacheNames="employeeById", key="#id")` on `update()` to refresh without eviction
  - `@CacheEvict(cacheNames="employeeById", key="#id")` on `delete()`
- The `EmployeeEntity.department` field is a `@Enumerated(EnumType.STRING)` column; valid values are: `HR`, `IT`, `FINANCE`, `OPERATIONS`, `MARKETING`, `LEGAL`.

## Data and Migration Rules
- Keep DDL changes in Flyway migrations only; do not depend on automatic schema creation.
- Maintain compatibility with JPA validate mode configured in [employee-platform-launcher/src/main/resources/application.yaml](employee-platform-launcher/src/main/resources/application.yaml).
- Existing baseline migration example: [employee-platform-launcher/src/main/resources/db/migration/V1__create_employee_table.sql](employee-platform-launcher/src/main/resources/db/migration/V1__create_employee_table.sql).

## Observability and Logging Rules
- Preserve correlation and trace propagation behavior in [employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/filter/CorrelationIdFilter.java](employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/filter/CorrelationIdFilter.java).
- `ProductionTraceBodyFilter` captures request/response bodies as span attributes (max 2 KB, masked fields: `password`, `token`, `secret`, `authorization`) and sets `X-Trace-Id` on responses — preserve this behavior in [employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/filter/ProductionTraceBodyFilter.java](employee-platform-launcher/src/main/java/io/growtogether/employee/launcher/filter/ProductionTraceBodyFilter.java).
- Preserve log pattern fields for trace and correlation IDs in [employee-platform-launcher/src/main/resources/logback-spring.xml](employee-platform-launcher/src/main/resources/logback-spring.xml).
- If changing logging appenders, verify no duplicated console output is introduced.

## Documentation Links
- Existing general Spring template docs: [HELP.md](HELP.md).
- Link to concrete implementation files above instead of duplicating large examples here.

## Change Checklist for Agents
- Keep edits scoped to the correct module boundary.
- Add or update tests when behavior changes.
- If changing API contracts, update DTOs, validation, controller behavior, and exception mapping together.
- If changing persistence, update entity, repository usage, and Flyway migration as one unit.
