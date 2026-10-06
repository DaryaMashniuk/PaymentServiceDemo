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
    private static final String OTHER_ACCOUNT = "0000000999";
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    @Autowired
    private TransactionRepository repository;

    private OffsetDateTime at(int y, int m, int d, int h) {
        return OffsetDateTime.of(y, m, d, h, 0, 0, 0, ZONE);
    }

    private Transaction tx(String account,String sum, String sumUsd, OffsetDateTime date,
                           ExpenseCategory cat, boolean exceeded) {
        return Transaction.builder()
                .accountFrom(account)
                .accountTo(COUNTERPARTY)
                .currencyShortname(Currency.KZT)
                .sum(new BigDecimal(sum))
                .expenseCategory(cat)
                .datetime(date)
                .sumUsd(new BigDecimal(sumUsd))
                .limitExceeded(exceeded)
                .build();
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
    @DisplayName("findAllByAccountFromAndLimitExceededTrue Tests")
    class FindAllByAccountFromAndLimitExceededTests {

        @Test
        @DisplayName("Should return exceeded transactions ONLY for the specified account")
        void shouldReturnExceededTransactionsOnlyForTargetAccount() {
            repository.save(tx("500.00", "500.00", at(2026, 1, 2, 10), ExpenseCategory.PRODUCT, false));
            repository.save(tx("600.00", "600.00", at(2026, 1, 3, 10), ExpenseCategory.PRODUCT, true));

            repository.save(tx(OTHER_ACCOUNT, "700.00", "700.00", at(2026, 1, 3, 11), ExpenseCategory.PRODUCT, true));

            List<Transaction> result = repository.findAllByAccountFromAndLimitExceededTrue(ACCOUNT);

            assertThat(result)
                    .hasSize(1)
                    .allMatch(t -> t.getAccountFrom().equals(ACCOUNT) && t.getLimitExceeded());
            assertThat(result.get(0).getSumUsd()).isEqualByComparingTo("600.00");
        }

        @Test
        @DisplayName("Should return empty list if account has no exceeded transactions")
        void shouldReturnEmptyWhenNoExceededForAccount() {
            repository.save(tx("100.00", "100.00", at(2026, 1, 2, 10), ExpenseCategory.PRODUCT, false));
            repository.save(tx(OTHER_ACCOUNT, "900.00", "900.00", at(2026, 1, 3, 10), ExpenseCategory.PRODUCT, true));

            List<Transaction> result = repository.findAllByAccountFromAndLimitExceededTrue(ACCOUNT);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findTransactionsForUpdate Tests")
    class FindTransactionsForUpdateTests {

        @Test
        @DisplayName("Should return only previous transactions within month and exclude current tx date")
        void shouldReturnOnlyPreviousTransactionsInMonth() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = at(2026, 2, 1, 0);
            OffsetDateTime currentTxTime = at(2026, 1, 15, 12);

            repository.save(tx("100.00", "100.00", at(2026, 1, 10, 10), ExpenseCategory.PRODUCT, false));
            repository.save(tx("200.00", "200.00", at(2026, 1, 14, 10), ExpenseCategory.PRODUCT, false));
            repository.save(tx(OTHER_ACCOUNT, "500.00", "500.00", at(2026, 1, 10, 10), ExpenseCategory.PRODUCT, false));
            repository.save(tx("300.00", "300.00", at(2026, 1, 10, 10), ExpenseCategory.SERVICE, false));
            repository.save(tx("400.00", "400.00", at(2025, 12, 31, 23), ExpenseCategory.PRODUCT, false));
            repository.save(tx("500.00", "500.00", at(2026, 1, 16, 10), ExpenseCategory.PRODUCT, false));

            List<Transaction> result = repository.findTransactionsForUpdate(
                    ACCOUNT,
                    ExpenseCategory.PRODUCT.name(),
                    monthStart,
                    monthEnd,
                    currentTxTime
            );

            assertThat(result)
                    .hasSize(2)
                    .extracting(Transaction::getSumUsd)
                    .containsExactly(new BigDecimal("100.00"), new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("Boundary: Transaction at the exact moment of currentDate is excluded")
        void shouldExcludeTransactionAtExactCurrentDate() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = at(2026, 2, 1, 0);
            OffsetDateTime currentTxTime = at(2026, 1, 10, 0);

            repository.save(tx("100.00", "100.00", currentTxTime, ExpenseCategory.PRODUCT, false));

            List<Transaction> result = repository.findTransactionsForUpdate(
                    ACCOUNT,
                    ExpenseCategory.PRODUCT.name(),
                    monthStart,
                    monthEnd,
                    currentTxTime
            );

            assertThat(result).isEmpty();
        }
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