package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LimitRepository
        extends JpaRepository<Limit, Long> {

    @Query(value = """
        SELECT l.*
        FROM limits l
        JOIN (
            SELECT
                account_from,
                expense_category,
                MAX(limit_datetime) AS max_limit_datetime
            FROM limits
            WHERE account_from = :accountFrom
              AND expense_category = :category
              AND limit_datetime <= :transactionDatetime
            GROUP BY account_from, expense_category
        ) latest
          ON latest.account_from = l.account_from
         AND latest.expense_category = l.expense_category
         AND latest.max_limit_datetime = l.limit_datetime
        WHERE l.account_from = :accountFrom
          AND l.expense_category = :category
        ORDER BY l.id DESC
        LIMIT 1
        """,
            nativeQuery = true)
    Optional<Limit> findLatestValidLimit(
            @Param("accountFrom") String accountFrom,
            @Param("category") String category,
            @Param("transactionDatetime") OffsetDateTime transactionDatetime
    );

    @Query("""
        select l.limitSum
        from Limit l
        where l.accountFrom = :account
          and l.expenseCategory = :category
          and l.limitDatetime = (
              select max(l2.limitDatetime)
              from Limit l2
              where l2.accountFrom = :account
                and l2.expenseCategory = :category
          )
        """)
    Optional<BigDecimal> findLatestLimitSum(
            @Param("account") String account,
            @Param("category") String category
    );

    List<Limit> findAllByAccountFromOrderByLimitDatetimeDesc(String account);
}