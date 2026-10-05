package by.mashnyuk.paymentServiceDemo.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    @Test
    void shouldCreateTransactionWithRequiredFields() {
        OffsetDateTime datetime =
                OffsetDateTime.parse("2026-01-15T10:00:00+06:00");

        Transaction transaction = new Transaction(
                1L,
                "0000000123",
                "9999999999",
                Currency.KZT,
                new BigDecimal("10000.45"),
                ExpenseCategory.PRODUCT,
                datetime,
                new BigDecimal("20.15"),
                false
        );

        assertThat(transaction.getAccountFrom())
                .isEqualTo("0000000123");

        assertThat(transaction.getAccountTo())
                .isEqualTo("9999999999");

        assertThat(transaction.getCurrencyShortname())
                .isEqualTo(Currency.KZT);

        assertThat(transaction.getSum())
                .isEqualByComparingTo("10000.45");

        assertThat(transaction.getExpenseCategory())
                .isEqualTo(ExpenseCategory.PRODUCT);

        assertThat(transaction.getDatetime())
                .isEqualTo(datetime);

        assertThat(transaction.getSumUsd())
                .isEqualByComparingTo("20.15");

        assertThat(transaction.getLimitExceeded())
                .isFalse();
    }

    @Test
    void shouldAllowChangingLimitExceededFlag() {
        Transaction transaction = new Transaction(
                1L,
                "0000000123",
                "9999999999",
                Currency.KZT,
                new BigDecimal("100.00"),
                ExpenseCategory.SERVICE,
                OffsetDateTime.now(),
                new BigDecimal("0.75"),
                false
        );

        transaction.setLimitExceeded(true);

        assertThat(transaction.getLimitExceeded())
                .isTrue();
    }
}