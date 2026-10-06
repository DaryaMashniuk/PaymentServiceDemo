# Database Migration Skill for Payment Service

## Purpose
This skill provides conventions, rules, and code templates for creating and maintaining Liquibase migrations in `paymentServiceDemo` using a **schema-first approach** with PostgreSQL 17 and **YAML changelogs**.

## Key Project Conventions

1. **Schema-First Approach**: Always update the database schema via Liquibase YAML changelogs *before* creating or modifying JPA/Hibernate entities.
2. **File Organization & Naming Rules**:
    - Location: `src/main/resources/db/changelog/`
    - Master Changelog: `db/changelog/db.changelog-master.yaml`
    - Changelog Naming Convention: `src/main/resources/db/changelog/changesets/NN-<short-description>.yaml` (e.g., `06-add-new-table.yaml`).
3. **Changeset Standard**:
    - Every changeset MUST specify `author: darya` and an incremental/descriptive `id` (e.g., `id: 06-add-new-table`).
    - Use standard Liquibase YAML format.
    - Always specify explicit data types (`NUMERIC(18,2)`, `TIMESTAMPTZ`, `VARCHAR(255)`, `BIGINT`).
    - Explicit constraint naming: `uk_<table_name>_<columns>` and `idx_<table_name>_<columns>`.
4. **Data Types Mapping**:
    - Financial Amounts: `NUMERIC(18,2)` $\rightarrow$ Java `BigDecimal` (scale 2, `RoundingMode.HALF_UP`)
    - Datetime with Time Zone: `TIMESTAMPTZ` $\rightarrow$ Java `OffsetDateTime`
    - Date only: `DATE` $\rightarrow$ Java `LocalDate`
    - Category / Enums: `VARCHAR(20)` $\rightarrow$ Java `ExpenseCategory` / `Currency`

---

## Standard Migration Workflow

### Step 1: Create a YAML Changeset File
Place the file in `src/main/resources/db/changelog/changesets/`.

**Example: `src/main/resources/db/changelog/changesets/05-create-monthly-spending-table.yaml`**

```yaml
databaseChangeLog:
  - changeSet:
      id: 05-create-monthly-spending-table
      author: darya
      changes:
        - createTable:
            tableName: monthly_spending
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: account_from
                  type: VARCHAR(10)
                  constraints:
                    nullable: false
              - column:
                  name: expense_category
                  type: VARCHAR(20)
                  constraints:
                    nullable: false
              - column:
                  name: month_start
                  type: DATE
                  constraints:
                    nullable: false
              - column:
                  name: limit_sum
                  type: NUMERIC(18,2)
                  constraints:
                    nullable: false
              - column:
                  name: spent_usd
                  type: NUMERIC(18,2)
                  defaultValueNumeric: 0.00
                  constraints:
                    nullable: false
              - column:
                  name: limit_exceeded
                  type: BOOLEAN
                  defaultValueBoolean: false
                  constraints:
                    nullable: false

        - addUniqueConstraint:
            tableName: monthly_spending
            columnNames: account_from, expense_category, month_start
            constraintName: uk_monthly_spending_acc_cat_month

        - createIndex:
            tableName: monthly_spending
            indexName: idx_monthly_spending_acc_month
            columns:
              - column:
                  name: account_from
              - column:
                  name: month_start
```

### Step 2: Register Changeset in Master Changelog

Include the YAML changeset in src/main/resources/db/changelog/db.changelog-master.yaml:

```Yaml
databaseChangeLog:
  - include:
      file: db/changelog/changesets/05-create-monthly-spending-table.yaml
```

### Step 3: Create / Update the JPA Entity

Ensure JPA annotations strictly match the Liquibase schema definition:

```Java
package by.mashnyuk.paymentServiceDemo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "monthly_spending",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_monthly_spending_acc_cat_month",
                        columnNames = {"account_from", "expense_category", "month_start"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySpending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_from", nullable = false, length = 10)
    private String accountFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false, length = 20)
    private ExpenseCategory expenseCategory;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "limit_sum", nullable = false, precision = 18, scale = 2)
    private BigDecimal limitSum;

    @Builder.Default
    @Column(name = "spent_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal spentUsd = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "limit_exceeded", nullable = false)
    private Boolean limitExceeded = false;
}
```

### Step 4: Verification Command

Run test verification to ensure Liquibase migrations execute successfully against Testcontainers PostgreSQL:

```Bash
./mvnw clean test -Dtest=*RepositoryTest
```

