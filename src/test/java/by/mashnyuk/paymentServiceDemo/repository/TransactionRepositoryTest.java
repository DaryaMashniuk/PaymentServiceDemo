package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.AbstractIntegrationTest;
import by.mashnyuk.paymentServiceDemo.model.Currency;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest extends AbstractIntegrationTest {

    private static final String ACCOUNT = "0000000123";
    private static final String COUNTERPARTY = "9999999999";
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    @Autowired
    private TransactionRepository repository;

    private OffsetDateTime at(int y, int m, int d, int h) {
        return OffsetDateTime.of(y, m, d, h, 0, 0, 0, ZONE);
    }

    private Transaction tx(String sum, String sumUsd, OffsetDateTime date,
                           ExpenseCategory cat, boolean exceeded) {
        return Transaction.builder()
                .accountFrom(ACCOUNT)
                .accountTo(COUNTERPARTY)
                .currencyShortname(Currency.KZT)
                .sum(new BigDecimal(sum))
                .expenseCategory(cat)
                .datetime(date)
                .sumUsd(new BigDecimal(sumUsd))
                .limitExceeded(exceeded)
                .build();
    }

    @Nested
    @DisplayName("Find Limit Exceeded Transactions Tests")
    class FindLimitExceededTests {

        @Test
        @DisplayName("Should return only transactions with limit_exceeded = true")
        void shouldReturnOnlyExceededTransactions() {
            OffsetDateTime date = at(2026, 1, 3, 10);
            repository.save(tx("500.00", "500.00", date, ExpenseCategory.PRODUCT, false));
            repository.save(tx("600.00", "600.00", date.plusHours(1), ExpenseCategory.PRODUCT, true));
            repository.save(tx("700.00", "700.00", date.plusHours(2), ExpenseCategory.PRODUCT, true));

            List<Transaction> exceeded = repository.findAllByLimitExceededTrue();

            assertThat(exceeded)
                    .hasSize(2)
                    .allMatch(Transaction::getLimitExceeded);
        }

        @Test
        @DisplayName("Should return an empty list when nothing exceeded the limit")
        void shouldReturnEmptyWhenNothingExceeded() {
            repository.save(tx("100.00", "100.00",
                    at(2026, 1, 3, 10), ExpenseCategory.PRODUCT, false));

            List<Transaction> exceeded = repository.findAllByLimitExceededTrue();

            assertThat(exceeded).isEmpty();
        }
    }
}