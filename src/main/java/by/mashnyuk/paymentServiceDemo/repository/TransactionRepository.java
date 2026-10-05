package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    @Query(value = """
        SELECT * FROM transactions t
        WHERE t.account_from = :account
          AND t.expense_category = :category
          AND t.datetime >= :monthStart
          AND t.datetime <  :monthEnd
          AND t.datetime <  :currentDate
        ORDER BY t.datetime, t.id
        FOR UPDATE
        """, nativeQuery = true)
    List<Transaction> findTransactionsForUpdate(
            @Param("account") String account,
            @Param("category") String category,
            @Param("monthStart") OffsetDateTime monthStart,
            @Param("monthEnd") OffsetDateTime monthEnd,
            @Param("currentDate") OffsetDateTime currentDate);

    List<Transaction> findByLimitExceededTrueAndDatetimeBetween(
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<Transaction> findAllByLimitExceededTrue();
}
