package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.model.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LimitRepository extends JpaRepository<Limit, Long> {

    @Query(value = """
            SELECT l.*
            FROM limits l
            WHERE l.account_from = :accountFrom
              AND l.expense_category = :category
              AND l.limit_datetime <= :transactionDatetime
            ORDER BY l.limit_datetime DESC, l.id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Limit> findLatestValidLimit(
            @Param("accountFrom") String accountFrom,
            @Param("category") String category,
            @Param("transactionDatetime") OffsetDateTime transactionDatetime
    );

    @Query("""
            SELECT l.limitSum
            FROM Limit l
            WHERE l.accountFrom = :account
              AND l.expenseCategory = :category
              AND l.limitDatetime = (
                  SELECT MAX(l2.limitDatetime)
                  FROM Limit l2
                  WHERE l2.accountFrom = :account
                    AND l2.expenseCategory = :category
              )
            """)
    Optional<BigDecimal> findLatestLimitSum(
            @Param("account") String account,
            @Param("category") String category
    );

    List<Limit> findAllByAccountFromOrderByLimitDatetimeDesc(String account);
}