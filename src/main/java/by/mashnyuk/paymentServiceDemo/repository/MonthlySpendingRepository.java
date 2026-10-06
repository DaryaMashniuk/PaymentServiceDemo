package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.MonthlySpending;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface MonthlySpendingRepository extends JpaRepository<MonthlySpending, Long> {

    Optional<MonthlySpending> findByAccountFromAndExpenseCategoryAndMonthStart(
            String accountFrom,
            ExpenseCategory expenseCategory,
            LocalDate monthStart
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ms FROM MonthlySpending ms
            WHERE ms.accountFrom = :account
              AND ms.expenseCategory = :category
              AND ms.monthStart = :monthStart
            """)
    Optional<MonthlySpending> findForUpdateByAccountAndCategoryAndMonth(
            @Param("account") String account,
            @Param("category") ExpenseCategory category,
            @Param("monthStart") LocalDate monthStart
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
            """, nativeQuery = true)
    int createIfAbsent(
            @Param("account") String account,
            @Param("category") String category,
            @Param("monthStart") LocalDate monthStart,
            @Param("limitSum") BigDecimal limitSum
    );
}