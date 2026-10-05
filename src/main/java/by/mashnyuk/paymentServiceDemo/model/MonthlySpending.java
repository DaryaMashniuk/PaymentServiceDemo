package by.mashnyuk.paymentServiceDemo.model;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(
        name = "monthly_spending",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_monthly_spending_acc_cat_month",
                        columnNames = {
                                "account_from",
                                "expense_category",
                                "month_start"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_monthly_spending_acc_month",
                        columnList = "account_from, month_start"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySpending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "account_from",
            nullable = false,
            length = 10
    )
    private String accountFrom;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "expense_category",
            nullable = false,
            length = 20
    )
    private ExpenseCategory expenseCategory;

    @Column(
            name = "month_start",
            nullable = false
    )
    private LocalDate monthStart;

    @Column(
            name = "limit_sum",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal limitSum;

    @Builder.Default
    @Column(
            name = "spent_usd",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal spentUsd = BigDecimal.ZERO;

    @Builder.Default
    @Column(
            name = "limit_exceeded",
            nullable = false
    )
    private Boolean limitExceeded = false;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    @Builder.Default
    private Long version = 0L;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MonthlySpending that = (MonthlySpending) o;

        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}