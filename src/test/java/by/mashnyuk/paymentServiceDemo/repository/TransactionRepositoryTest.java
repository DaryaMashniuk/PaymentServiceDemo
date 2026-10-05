package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.AbstractIntegrationTest;
import by.mashnyuk.paymentServiceDemo.model.Currency;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest extends AbstractIntegrationTest {

    private static final String ACCOUNT = "0000000123";
    private static final String COUNTERPARTY = "9999999999";
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private PlatformTransactionManager txManager;


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

    private Transaction tx(String acc, String sum, String sumUsd, OffsetDateTime date,
                           ExpenseCategory cat, boolean exceeded) {
        return Transaction.builder()
                .accountFrom(acc)
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
    @DisplayName("Find Transactions For Update Tests")
    class FindTransactionsForUpdateTests {

        @Test
        @DisplayName("Should return only transactions within the month and before the current transaction")
        void shouldFindTransactionsBeforeCurrentTransaction() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);
            OffsetDateTime currentDate = at(2026, 1, 13, 10);

            repository.save(tx("100.00", "500.00",
                    currentDate.minusDays(2), ExpenseCategory.PRODUCT, false));
            repository.save(tx("200.00", "600.00",
                    currentDate.minusDays(1), ExpenseCategory.PRODUCT, false));
            repository.save(tx("300.00", "700.00",
                    at(2025, 12, 31, 23), ExpenseCategory.PRODUCT, false));
            repository.save(tx("400.00", "800.00",
                    currentDate.plusDays(1), ExpenseCategory.PRODUCT, false));

            List<Transaction> transactions = repository.findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), monthStart, monthEnd, currentDate);

            assertThat(transactions)
                    .hasSize(2)
                    .extracting(Transaction::getSumUsd)
                    .containsExactlyInAnyOrder(
                            new BigDecimal("500.00"),
                            new BigDecimal("600.00"));
        }

        @Test
        @DisplayName("Should aggregate the total USD expense correctly")
        void shouldAggregateSumUsdCorrectly() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);
            OffsetDateTime currentDate = at(2026, 1, 13, 10);

            repository.save(tx("100.00", "500.00",
                    currentDate.minusDays(2), ExpenseCategory.PRODUCT, false));
            repository.save(tx("200.00", "600.00",
                    currentDate.minusDays(1), ExpenseCategory.PRODUCT, false));

            List<Transaction> transactions = repository.findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), monthStart, monthEnd, currentDate);

            BigDecimal total = transactions.stream()
                    .map(Transaction::getSumUsd)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(total).isEqualByComparingTo("1100.00");
        }

        @Test
        @DisplayName("Boundary: transaction on the month start boundary (first second) is included")
        void shouldIncludeTransactionOnMonthStartBoundary() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);

            repository.save(tx("100.00", "500.00",
                    monthStart, ExpenseCategory.PRODUCT, false));

            List<Transaction> transactions = repository.findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), monthStart, monthEnd, monthEnd);

            assertThat(transactions).hasSize(1);
        }

        @Test
        @DisplayName("Boundary: transaction on the month end boundary (last second) is included")
        void shouldIncludeTransactionOnMonthEndBoundary() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);

            repository.save(tx("100.00", "500.00",
                    monthEnd.minusSeconds(1), ExpenseCategory.PRODUCT, false));

            List<Transaction> transactions = repository.findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), monthStart, monthEnd, monthEnd);

            assertThat(transactions).hasSize(1);
        }

        @Test
        @DisplayName("Boundary: transaction exactly at the limit moment is NOT included (it is the current one)")
        void shouldExcludeTransactionAtExactLimitMoment() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);
            OffsetDateTime limitMoment = at(2026, 1, 10, 0);

            repository.save(tx("100.00", "500.00",
                    limitMoment, ExpenseCategory.PRODUCT, false));

            List<Transaction> transactions = repository.findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), monthStart, monthEnd, limitMoment);

            assertThat(transactions).isEmpty();
        }

        @Test
        @DisplayName("Boundary: transaction one second before the limit moment IS included")
        void shouldIncludeTransactionOneSecondBeforeLimitMoment() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);
            OffsetDateTime limitMoment = at(2026, 1, 10, 0);

            repository.save(tx("100.00", "500.00",
                    limitMoment.minusSeconds(1), ExpenseCategory.PRODUCT, false));

            List<Transaction> transactions = repository.findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), monthStart, monthEnd, limitMoment);

            assertThat(transactions).hasSize(1);
        }

        @Test
        @DisplayName("Should distinguish expense categories")
        void shouldNotMixCategories() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);
            OffsetDateTime currentDate = at(2026, 1, 13, 10);

            repository.save(tx("100.00", "500.00",
                    currentDate.minusDays(1), ExpenseCategory.PRODUCT, false));
            repository.save(tx("200.00", "600.00",
                    currentDate.minusDays(1), ExpenseCategory.SERVICE, false));

            List<Transaction> productTx = repository.findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), monthStart, monthEnd, currentDate);

            assertThat(productTx)
                    .hasSize(1)
                    .extracting(Transaction::getSumUsd)
                    .containsExactly(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Should distinguish clients by account_from")
        void shouldNotMixAccounts() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);
            OffsetDateTime currentDate = at(2026, 1, 13, 10);

            repository.save(tx("100.00", "500.00",
                    currentDate.minusDays(1), ExpenseCategory.PRODUCT, false));
            repository.save(tx("5555555555", "200.00", "600.00",
                    currentDate.minusDays(1), ExpenseCategory.PRODUCT, false));

            List<Transaction> transactions = repository.findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), monthStart, monthEnd, currentDate);

            assertThat(transactions).hasSize(1);
        }

        @Test
        @DisplayName("Should return an empty list when there are no transactions for the month")
        void shouldReturnEmptyWhenNoTransactions() {
            OffsetDateTime monthStart = at(2026, 1, 1, 0);
            OffsetDateTime monthEnd = monthStart.plusMonths(1);

            List<Transaction> transactions = repository.findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), monthStart, monthEnd, monthEnd);

            assertThat(transactions).isEmpty();
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