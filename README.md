# RealWorld Spring Boot Example App

[![Actions](https://github.com/alvescoders-br/spring-boot-realworld-example-app/actions/workflows/gradle.yml/badge.svg)](https://github.com/alvescoders-br/spring-boot-realworld-example-app/actions/workflows/gradle.yml)

Backend RealWorld/Conduit implemented with Java, Spring Boot, REST, GraphQL,
Spring Security, Spring Data JPA/Hibernate, Flyway and PostgreSQL.

This README was refreshed as part of issue #42 after the refactoring audit, so
it reflects the current Java/JPA/PostgreSQL architecture instead of the legacy
MyBatis/SQLite baseline.

## Runtime

- Java 25
- Spring Boot 4.0.3
- Gradle 9.3.1 wrapper
- PostgreSQL 18 via Docker Compose
- Flyway database migrations
- REST + GraphQL DGS
- OpenAPI/Swagger UI via springdoc
- Micrometer/Prometheus metrics and OpenTelemetry traces for the local LGTM stack

## Architecture

- `api`: REST controllers, exception handling and security configuration.
- `graphql`: GraphQL queries, mutations and HTTP transport configuration.
- `core`: domain entities, repositories and domain services.
- `application`: query services, DTOs and pagination helpers.
- `infrastructure`: Spring Data JPA adapters and read services.

Persistence is implemented with Spring Data JPA/Hibernate over PostgreSQL.
Schema changes are versioned in `src/main/resources/db/migration`.

## Local Run

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the application:

```bash
set SPRING_PROFILES_ACTIVE=postgres
set REALWORLD_POSTGRES_URL=jdbc:postgresql://localhost:55432/realworld
set REALWORLD_POSTGRES_USERNAME=realworld
set REALWORLD_POSTGRES_PASSWORD=realworld-local-password
gradlew.bat bootRun
```

Smoke check:

```bash
curl http://localhost:8080/tags
```

The API base URL is `http://localhost:8080`; there is no `/api` prefix.

## API Documentation

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- GraphQL endpoint: `http://localhost:8080/graphql`
- GraphQL schema: `src/main/resources/schema/schema.graphqls`

## Docker And Observability

The full local stack starts the app, PostgreSQL and LGTM components:

```bash
docker compose up -d
```

Services:

- App: `http://localhost:8080`
- Grafana: `http://localhost:3000`
- Loki: `http://localhost:3100`
- Tempo: `http://localhost:33200`
- Mimir: `http://localhost:9909`
- Alloy: `http://localhost:12345`

The app exposes `GET /actuator/health`, `GET /actuator/info` and
`GET /actuator/prometheus` for local operational checks. Grafana Alloy scrapes
the Prometheus endpoint and remote-writes metrics to Mimir; traces are exported
to Tempo through OTLP.

The operations validation script starts Compose, waits for readiness, checks the
endpoint counter, validates startup and shutdown log markers, and tears the
stack down:

```bash
py scripts/validate-operations.py
```

## Tests

Run the main suite:

```bash
gradlew.bat test
```

The suite expects PostgreSQL to be reachable when tests use the `postgres`
profile. For local execution, start the Compose database first:

```bash
docker compose up -d postgres
gradlew.bat test
```

Run Playwright API E2E smoke tests:

```bash
gradlew.bat playwrightE2e
```

Run mutation testing:

```bash
gradlew.bat pitest
```

## Formatting

```bash
gradlew.bat spotlessApply
```
