package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.AbstractIntegrationTest;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.MonthlySpending;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MonthlySpendingRepositoryConcurrencyTest
        extends AbstractIntegrationTest {

    @Autowired
    private MonthlySpendingRepository spendingRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private ExecutorService executor;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            );
        }
    }

    @Test
    @DisplayName(
            "parallel increments must be serialized by PostgreSQL"
    )
    void shouldSerializeParallelIncrements()
            throws Exception {

        String account = "0000000010";

        LocalDate month =
                LocalDate.of(2026, 1, 1);

        runInNewTransaction(() ->
                spendingRepository.save(
                        MonthlySpending.builder()
                                .accountFrom(account)
                                .expenseCategory(
                                        ExpenseCategory.PRODUCT
                                )
                                .monthStart(month)
                                .limitSum(
                                        new BigDecimal("1000.00")
                                )
                                .spentUsd(
                                        new BigDecimal("500.00")
                                )
                                .limitExceeded(false)
                                .build()
                )
        );

        int threadCount = 4;

        executor =
                Executors.newFixedThreadPool(
                        threadCount
                );

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<?>> futures =
                new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {

            futures.add(
                    executor.submit(() -> {

                        start.await();

                        runInNewTransaction(() ->
                                spendingRepository.incrementSpent(
                                        account,
                                        ExpenseCategory.PRODUCT.name(),
                                        month,
                                        new BigDecimal("150.00")
                                )
                        );

                        return null;
                    })
            );
        }

        start.countDown();

        for (Future<?> future : futures) {
            future.get(
                    15,
                    TimeUnit.SECONDS
            );
        }

        MonthlySpending result =
                spendingRepository
                        .findByAccountFromAndExpenseCategoryAndMonthStart(
                                account,
                                ExpenseCategory.PRODUCT,
                                month
                        )
                        .orElseThrow();

        assertThat(result.getSpentUsd())
                .isEqualByComparingTo("1100.00");

        assertThat(result.getLimitExceeded())
                .isTrue();
    }

    @Test
    @DisplayName(
            "parallel increments must not lose updates"
    )
    void shouldNotLoseUpdates()
            throws Exception {

        String account = "0000000011";

        LocalDate month =
                LocalDate.of(2026, 1, 1);

        int threadCount = 10;

        runInNewTransaction(() ->
                spendingRepository.save(
                        MonthlySpending.builder()
                                .accountFrom(account)
                                .expenseCategory(
                                        ExpenseCategory.SERVICE
                                )
                                .monthStart(month)
                                .limitSum(
                                        new BigDecimal("10000.00")
                                )
                                .spentUsd(
                                        BigDecimal.ZERO
                                )
                                .limitExceeded(false)
                                .build()
                )
        );

        executor =
                Executors.newFixedThreadPool(
                        threadCount
                );

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<?>> futures =
                new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {

            futures.add(
                    executor.submit(() -> {

                        start.await();

                        runInNewTransaction(() ->
                                spendingRepository.incrementSpent(
                                        account,
                                        ExpenseCategory.SERVICE.name(),
                                        month,
                                        new BigDecimal("100.00")
                                )
                        );

                        return null;
                    })
            );
        }

        start.countDown();

        for (Future<?> future : futures) {
            future.get(
                    15,
                    TimeUnit.SECONDS
            );
        }

        MonthlySpending result =
                spendingRepository
                        .findByAccountFromAndExpenseCategoryAndMonthStart(
                                account,
                                ExpenseCategory.SERVICE,
                                month
                        )
                        .orElseThrow();

        assertThat(result.getSpentUsd())
                .isEqualByComparingTo("1000.00");

        assertThat(result.getLimitExceeded())
                .isFalse();
    }

    private void runInNewTransaction(
            Runnable action
    ) {

        TransactionTemplate template =
                new TransactionTemplate(
                        transactionManager
                );

        template.executeWithoutResult(
                status -> action.run()
        );
    }
}