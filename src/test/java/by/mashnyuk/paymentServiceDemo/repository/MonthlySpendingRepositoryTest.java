package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.AbstractIntegrationTest;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.MonthlySpending;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MonthlySpendingRepositoryTest extends AbstractIntegrationTest {

    private static final String ACCOUNT = "0000000123";
    private static final LocalDate MONTH = LocalDate.of(2026, 1, 1);

    @Autowired
    private MonthlySpendingRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("createIfAbsent: Should insert row if not present, and DO NOTHING on second call")
    void shouldCreateIfAbsentAndIgnoreOnConflict() {
        int inserted = repository.createIfAbsent(
                ACCOUNT,
                ExpenseCategory.PRODUCT.name(),
                MONTH,
                new BigDecimal("1000.00")
        );

        assertThat(inserted).isEqualTo(1);

        Optional<MonthlySpending> created = repository
                .findByAccountFromAndExpenseCategoryAndMonthStart(ACCOUNT, ExpenseCategory.PRODUCT, MONTH);

        assertThat(created).isPresent();
        assertThat(created.get().getLimitSum()).isEqualByComparingTo("1000.00");
        assertThat(created.get().getSpentUsd()).isEqualByComparingTo("0.00");
        assertThat(created.get().getLimitExceeded()).isFalse();

        int secondInsert = repository.createIfAbsent(
                ACCOUNT,
                ExpenseCategory.PRODUCT.name(),
                MONTH,
                new BigDecimal("2000.00")
        );

        assertThat(secondInsert).isEqualTo(0);

        MonthlySpending afterSecondCall = repository
                .findByAccountFromAndExpenseCategoryAndMonthStart(ACCOUNT, ExpenseCategory.PRODUCT, MONTH)
                .orElseThrow();

        assertThat(afterSecondCall.getLimitSum()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Should isolate records by category and monthStart")
    void shouldIsolateRecordsByCategoryAndMonth() {
        repository.createIfAbsent(ACCOUNT, ExpenseCategory.PRODUCT.name(), MONTH, new BigDecimal("1000.00"));
        repository.createIfAbsent(ACCOUNT, ExpenseCategory.SERVICE.name(), MONTH, new BigDecimal("500.00"));
        repository.createIfAbsent(ACCOUNT, ExpenseCategory.PRODUCT.name(), MONTH.plusMonths(1), new BigDecimal("1000.00"));

        assertThat(repository.count()).isEqualTo(3);
    }
}