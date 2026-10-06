package by.mashnyuk.paymentServiceDemo.service.impl;

import by.mashnyuk.paymentServiceDemo.model.Currency;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Limit;
import by.mashnyuk.paymentServiceDemo.model.MonthlySpending;
import by.mashnyuk.paymentServiceDemo.model.Transaction;
import by.mashnyuk.paymentServiceDemo.repository.MonthlySpendingRepository;
import by.mashnyuk.paymentServiceDemo.repository.TransactionRepository;
import by.mashnyuk.paymentServiceDemo.service.ExchangeRateService;
import by.mashnyuk.paymentServiceDemo.service.LimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    private static final String ACCOUNT = "0000000123";
    private static final String COUNTERPARTY = "9999999999";
    private static final ZoneId APPLICATION_ZONE_ID = ZoneId.of("UTC");

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MonthlySpendingRepository monthlySpendingRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private LimitService limitService;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(
                transactionRepository,
                monthlySpendingRepository,
                exchangeRateService,
                limitService,
                APPLICATION_ZONE_ID
        );
    }

    private Transaction createTx(String sum, Currency currency, ExpenseCategory category, OffsetDateTime time) {
        return Transaction.builder()
                .accountFrom(ACCOUNT)
                .accountTo(COUNTERPARTY)
                .currencyShortname(currency)
                .sum(new BigDecimal(sum))
                .expenseCategory(category)
                .datetime(time)
                .build();
    }

    @Nested
    @DisplayName("Currency Conversion Tests")
    class CurrencyConversionTests {

        @Test
        @DisplayName("Should not convert sum if currency is USD")
        void shouldNotConvertIfCurrencyIsUsd() {
            OffsetDateTime txTime = OffsetDateTime.of(2026, 1, 10, 12, 0, 0, 0, ZoneOffset.UTC);
            Transaction tx = createTx("250.50", Currency.USD, ExpenseCategory.PRODUCT, txTime);

            Limit limit = Limit.builder().limitSum(new BigDecimal("1000.00")).build();
            when(limitService.getActualLimit(any(), any(), any())).thenReturn(limit);
            when(monthlySpendingRepository.findForUpdateByAccountAndCategoryAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(new MonthlySpending()));
            when(transactionRepository.findTransactionsForUpdate(any(), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Transaction processed = transactionService.processTransaction(tx);

            assertThat(processed.getSumUsd()).isEqualByComparingTo("250.50");
            verifyNoInteractions(exchangeRateService);
        }

        @Test
        @DisplayName("Should convert KZT to USD using ExchangeRateService")
        void shouldConvertKztToUsd() {
            OffsetDateTime txTime = OffsetDateTime.of(2026, 1, 10, 12, 0, 0, 0, ZoneOffset.UTC);
            Transaction tx = createTx("50000.00", Currency.KZT, ExpenseCategory.PRODUCT, txTime);

            when(exchangeRateService.getRate(LocalDate.of(2026, 1, 10), "KZT/USD"))
                    .thenReturn(new BigDecimal("0.0020"));

            Limit limit = Limit.builder().limitSum(new BigDecimal("1000.00")).build();
            when(limitService.getActualLimit(any(), any(), any())).thenReturn(limit);
            when(monthlySpendingRepository.findForUpdateByAccountAndCategoryAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(new MonthlySpending()));
            when(transactionRepository.findTransactionsForUpdate(any(), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Transaction processed = transactionService.processTransaction(tx);

            assertThat(processed.getSumUsd()).isEqualByComparingTo("100.00");
            verify(exchangeRateService).getRate(LocalDate.of(2026, 1, 10), "KZT/USD");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when exchange rate is unavailable or zero")
        void shouldThrowWhenRateIsUnavailable() {
            OffsetDateTime txTime = OffsetDateTime.of(2026, 1, 10, 12, 0, 0, 0, ZoneOffset.UTC);
            Transaction tx = createTx("1000.00", Currency.RUB, ExpenseCategory.SERVICE, txTime);

            when(exchangeRateService.getRate(any(), eq("RUB/USD"))).thenReturn(null);

            assertThatThrownBy(() -> transactionService.processTransaction(tx))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Exchange rate not available for pair: RUB/USD");
        }
    }

    @Nested
    @DisplayName("Limit Calculation and Boundary Tests (Specification Scenarios)")
    class LimitCalculationTests {

        private final OffsetDateTime txTime = OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);

        @Test
        @DisplayName("Boundary: Transaction exactly equals remaining limit (limit balance is 0.00) -> limitExceeded = false")
        void shouldNotExceedLimitWhenSumEqualsLimitExactly() {
            Transaction tx = createTx("500.00", Currency.USD, ExpenseCategory.PRODUCT, txTime);

            Limit limit = Limit.builder().limitSum(new BigDecimal("1000.00")).build();
            when(limitService.getActualLimit(any(), any(), any())).thenReturn(limit);
            when(monthlySpendingRepository.findForUpdateByAccountAndCategoryAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(new MonthlySpending()));

            Transaction previousTx = Transaction.builder().sumUsd(new BigDecimal("500.00")).build();
            when(transactionRepository.findTransactionsForUpdate(any(), any(), any(), any(), eq(txTime)))
                    .thenReturn(List.of(previousTx));

            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Transaction processed = transactionService.processTransaction(tx);

            assertThat(processed.getLimitExceeded()).isFalse();
        }

        @Test
        @DisplayName("Boundary: Transaction exceeds remaining limit by 0.01 USD -> limitExceeded = true")
        void shouldExceedLimitWhenTotalIsOneCentOver() {
            Transaction tx = createTx("500.01", Currency.USD, ExpenseCategory.PRODUCT, txTime);

            Limit limit = Limit.builder().limitSum(new BigDecimal("1000.00")).build();
            when(limitService.getActualLimit(any(), any(), any())).thenReturn(limit);
            when(monthlySpendingRepository.findForUpdateByAccountAndCategoryAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(new MonthlySpending()));

            Transaction previousTx = Transaction.builder().sumUsd(new BigDecimal("500.00")).build();
            when(transactionRepository.findTransactionsForUpdate(any(), any(), any(), any(), eq(txTime)))
                    .thenReturn(List.of(previousTx));

            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Transaction processed = transactionService.processTransaction(tx);

            assertThat(processed.getLimitExceeded()).isTrue();
        }

        @Test
        @DisplayName("Scenario from Spec: Multiple previous transactions are summed up correctly")
        void shouldSumMultiplePreviousTransactions() {
            Transaction tx = createTx("100.00", Currency.USD, ExpenseCategory.PRODUCT, txTime);

            Limit limit = Limit.builder().limitSum(new BigDecimal("1000.00")).build();
            when(limitService.getActualLimit(any(), any(), any())).thenReturn(limit);
            when(monthlySpendingRepository.findForUpdateByAccountAndCategoryAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(new MonthlySpending()));

            List<Transaction> previousTxs = List.of(
                    Transaction.builder().sumUsd(new BigDecimal("500.00")).build(),
                    Transaction.builder().sumUsd(new BigDecimal("450.00")).build()
            );
            when(transactionRepository.findTransactionsForUpdate(any(), any(), any(), any(), eq(txTime)))
                    .thenReturn(previousTxs);

            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Transaction processed = transactionService.processTransaction(tx);

            assertThat(processed.getLimitExceeded()).isTrue();
        }

        @Test
        @DisplayName("Should query previous transactions with correct month boundaries and pessimistic lock anchor")
        void shouldLockMonthlyAnchorAndQueryWithCorrectBoundaries() {
            OffsetDateTime txTimeMidMonth = OffsetDateTime.of(2026, 1, 15, 14, 30, 0, 0, ZoneOffset.UTC);
            Transaction tx = createTx("50.00", Currency.USD, ExpenseCategory.SERVICE, txTimeMidMonth);

            Limit limit = Limit.builder().limitSum(new BigDecimal("1000.00")).build();
            when(limitService.getActualLimit(any(), any(), any())).thenReturn(limit);
            when(monthlySpendingRepository.findForUpdateByAccountAndCategoryAndMonth(any(), any(), any()))
                    .thenReturn(Optional.of(new MonthlySpending()));
            when(transactionRepository.findTransactionsForUpdate(any(), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            transactionService.processTransaction(tx);

            LocalDate expectedMonthStart = LocalDate.of(2026, 1, 1);
            verify(monthlySpendingRepository).createIfAbsent(
                    ACCOUNT, ExpenseCategory.SERVICE.name(), expectedMonthStart, limit.getLimitSum()
            );
            verify(monthlySpendingRepository).findForUpdateByAccountAndCategoryAndMonth(
                    ACCOUNT, ExpenseCategory.SERVICE, expectedMonthStart
            );

            OffsetDateTime expectedStart = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime expectedEnd = OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

            verify(transactionRepository).findTransactionsForUpdate(
                    ACCOUNT, ExpenseCategory.SERVICE.name(), expectedStart, expectedEnd, txTimeMidMonth
            );
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        private final OffsetDateTime validTime = OffsetDateTime.now(ZoneOffset.UTC);

        @Test
        @DisplayName("Should throw IllegalArgumentException when transaction is null")
        void shouldThrowWhenTxIsNull() {
            assertThatThrownBy(() -> transactionService.processTransaction(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when accountFrom is blank")
        void shouldThrowWhenAccountIsBlank() {
            Transaction tx = createTx("100.00", Currency.USD, ExpenseCategory.PRODUCT, validTime);
            tx.setAccountFrom("   ");

            assertThatThrownBy(() -> transactionService.processTransaction(tx))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Account must not be blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when sum is negative")
        void shouldThrowWhenSumIsNegative() {
            Transaction tx = createTx("-10.00", Currency.USD, ExpenseCategory.PRODUCT, validTime);

            assertThatThrownBy(() -> transactionService.processTransaction(tx))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction sum must be greater than or equal to zero");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when currency is null")
        void shouldThrowWhenCurrencyIsNull() {
            Transaction tx = createTx("100.00", null, ExpenseCategory.PRODUCT, validTime);

            assertThatThrownBy(() -> transactionService.processTransaction(tx))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction currency must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when datetime is null")
        void shouldThrowWhenDatetimeIsNull() {
            Transaction tx = createTx("100.00", Currency.USD, ExpenseCategory.PRODUCT, null);

            assertThatThrownBy(() -> transactionService.processTransaction(tx))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction datetime must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when expenseCategory is null")
        void shouldThrowWhenCategoryIsNull() {
            Transaction tx = createTx("100.00", Currency.USD, null, validTime);

            assertThatThrownBy(() -> transactionService.processTransaction(tx))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Expense category must not be null");
        }
    }
}