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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(
                        name = "idx_transactions_acc_cat_date",
                        columnList = "account_from, expense_category, datetime"
                )
        }
)
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "account_from",
            nullable = false,
            length = 10
    )
    private String accountFrom;

    @Column(
            name = "account_to",
            nullable = false,
            length = 10
    )
    private String accountTo;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "currency_shortname",
            nullable = false,
            length = 3
    )
    private Currency currencyShortname;

    @Column(
            name = "sum",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal sum;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "expense_category",
            nullable = false,
            length = 20
    )
    private ExpenseCategory expenseCategory;

    @Column(
            name = "datetime",
            nullable = false
    )
    private OffsetDateTime datetime;

    @Column(
            name = "sum_usd",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal sumUsd;

    @Builder.Default
    @Column(
            name = "limit_exceeded",
            nullable = false
    )
    private Boolean limitExceeded = false;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Transaction that = (Transaction) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
