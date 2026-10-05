package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.MonthlySpending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface MonthlySpendingRepository
        extends JpaRepository<MonthlySpending, Long> {

    Optional<MonthlySpending>
    findByAccountFromAndExpenseCategoryAndMonthStart(
            String accountFrom,
            ExpenseCategory expenseCategory,
            LocalDate monthStart
    );

    @Modifying
    @Query(value = """
            INSERT INTO monthly_spending (
                account_from,
                expense_category,
                month_start,
                limit_sum,
                spent_usd,
                limit_exceeded,
                version
            )
            VALUES (
                :account,
                :category,
                :monthStart,
                :limitSum,
                0,
                false,
                0
            )
            ON CONFLICT (
                account_from,
                expense_category,
                month_start
            )
            DO NOTHING
            """,
            nativeQuery = true)
    int createIfAbsent(
            @Param("account") String account,
            @Param("category") ExpenseCategory category,
            @Param("monthStart") LocalDate monthStart,
            @Param("limitSum") BigDecimal limitSum
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query(value = """
            UPDATE monthly_spending
               SET spent_usd = spent_usd + :amount,
                   limit_exceeded =
                       (spent_usd + :amount) > limit_sum,
                   version = version + 1
             WHERE account_from = :account
               AND expense_category = :category
               AND month_start = :monthStart
            """,
            nativeQuery = true)
    int incrementSpent(
            @Param("account") String account,
            @Param("category") String category,
            @Param("monthStart") LocalDate monthStart,
            @Param("amount") BigDecimal amount
    );
}