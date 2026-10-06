# AGENTS.md — AI Agent Guidelines for Payment Service Demo

## 1. Project Overview & Architecture
This microservice processes client banking transactions in KZT, RUB, etc., enforces monthly USD spending limits separately for products (`product`) and services (`service`), manages currency exchange rates with fallback mechanisms, and exposes REST APIs.

### Key Business Rules:
- **Default Limit**: $1000 USD if no explicit limit was set for the current month.
- **Limit Exceeded Logic**: Historical limit set on `date_X` applies ONLY to transactions on or after `date_X`. Setting a new limit never alters past transactions' `limit_exceeded` flag.
- **Concurrency**: Transactions for the same client account must be thread-safe. Use `PESSIMISTIC_WRITE` DB locks on spending records to avoid race conditions.
- **Exchange Rates**: DB First -> External API (TwelveData) -> Fallback to `previous_close`.
- **Timezone**: All month boundaries are strictly computed in `ZoneOffset.UTC` using `java.time.Clock`.

## 2. Tech Stack
- **Java**: 21 / 25 LTS
- **Framework**: Spring Boot 3.3+ / 4.x (WebMVC with Virtual Threads enabled)
- **Database**: PostgreSQL 17, Liquibase migrations
- **Resilience & Observability**: Resilience4j CircuitBreaker, Micrometer + Prometheus Actuator
- **Testing**: JUnit 5, Mockito, AssertJ, Testcontainers, WireMock

## 3. Build & Test Commands
Before finalizing any task, run:
```bash
# Run unit & integration tests
./mvnw clean test

# Run full end-to-end E2E test (Requirement 6)
./mvnw verify -P integration-test