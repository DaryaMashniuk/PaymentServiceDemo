---
name: limit-testing
description: Use this skill when asked to write or extend unit and integration tests for monthly spending limits, limit_exceeded flag logic, or datetime edge cases in paymentServiceDemo.
---

# Limit Testing Skill for Payment Service

## Purpose
Provides conventions, strict rules, and code templates for writing unit and integration tests covering limit management, pessimistic locking, and transaction verification in `paymentServiceDemo`.

## Key Project Conventions
1. **Clock Manipulation**: Never use real system time in tests. Always inject `java.time.Clock` with `Clock.fixed(Instant, ZoneId)` to simulate boundary cases (e.g. 1st second of month, switch between months, leap years).
2. **Timezone**: All business month boundaries are computed strictly in UTC (or configured zone from `TimeConfig`).
3. **Assertions**: Use AssertJ (`assertThat(...)`). For `BigDecimal` comparisons, ALWAYS use `.isEqualByComparingTo(...)` to ignore scale differences (e.g. `1000.00` vs `1000`).
4. **Target Test Classes**:
   - Service Logic: `by.mashnyuk.paymentServiceDemo.service.impl.TransactionServiceImplTest`
   - External Integration: `by.mashnyuk.paymentServiceDemo.service.ExchangeRateExternalServiceTest`
   - Full End-to-End Test: `by.mashnyuk.paymentServiceDemo.integration.Case1E2ETest`

## Standard Test Patterns

### Pattern A: Testing Boundary Conditions with Fixed Clock
```java
@Test
@DisplayName("Should flag limit_exceeded = true when exact monthly limit is exceeded by 0.01 USD")
void shouldExceedLimitOnExactBoundary() {
    Instant fixedInstant = Instant.parse("2026-01-15T10:00:00Z");
    Clock clock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
    
    // Test implementation using explicit Clock instance
}