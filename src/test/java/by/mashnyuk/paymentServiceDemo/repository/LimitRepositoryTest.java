package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.AbstractIntegrationTest;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Limit;
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
class LimitRepositoryTest extends AbstractIntegrationTest {

    private static final String ACCOUNT = "0000000123";
    private static final ZoneOffset ZONE = ZoneOffset.ofHours(6);

    @Autowired
    private LimitRepository limitRepository;

    private OffsetDateTime at(int year, int month, int day) {
        return OffsetDateTime.of(year, month, day, 0, 0, 0, 0, ZONE);
    }

    private Limit limit(String sum, OffsetDateTime date, ExpenseCategory cat) {
        return Limit.builder()
                .accountFrom(ACCOUNT)
                .limitSum(new BigDecimal(sum))
                .limitDatetime(date)
                .limitCurrencyShortname("USD")
                .expenseCategory(cat)
                .build();
    }

    @Nested
    @DisplayName("Find Latest Valid Limit Tests")
    class FindLatestValidLimitTests {

        @Test
        @DisplayName("Should find the latest limit set before the transaction date")
        void shouldFindLatestLimitValidForTransactionDate() {
            limitRepository.save(limit("1000.00", at(2026, 1, 1), ExpenseCategory.PRODUCT));
            limitRepository.save(limit("2000.00", at(2026, 1, 10), ExpenseCategory.PRODUCT));

            var result = limitRepository.findLatestValidLimit(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), at(2026, 1, 11));

            assertThat(result).isPresent();
            assertThat(result.get().getLimitSum()).isEqualByComparingTo("2000.00");
            assertThat(result.get().getLimitDatetime()).isEqualTo(at(2026, 1, 10));
        }

        @Test
        @DisplayName("Should ignore a limit created after the transaction date")
        void shouldIgnoreLimitCreatedAfterTransaction() {
            limitRepository.save(limit("2000.00", at(2026, 1, 10), ExpenseCategory.PRODUCT));

            var result = limitRepository.findLatestValidLimit(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), at(2026, 1, 5));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return the limit set exactly on the transaction date (boundary)")
        void shouldReturnLimitOnExactTransactionDate() {
            limitRepository.save(limit("1000.00", at(2026, 1, 10), ExpenseCategory.PRODUCT));

            var result = limitRepository.findLatestValidLimit(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), at(2026, 1, 10));

            assertThat(result).isPresent();
            assertThat(result.get().getLimitSum()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("Should not mix categories: PRODUCT limit is not visible for SERVICE")
        void shouldNotMixCategories() {
            limitRepository.save(limit("1000.00", at(2026, 1, 1), ExpenseCategory.PRODUCT));

            var result = limitRepository.findLatestValidLimit(
                    ACCOUNT, ExpenseCategory.SERVICE.name(), at(2026, 1, 5));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty result when no limits exist at all")
        void shouldReturnEmptyWhenNoLimitsExist() {
            var result = limitRepository.findLatestValidLimit(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), at(2026, 1, 1));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Limit change within month: transaction after change sees the NEW limit")
        void shouldSeeNewLimitAfterChangeWithinMonth() {
            limitRepository.save(limit("1000.00", at(2026, 1, 1), ExpenseCategory.PRODUCT));
            limitRepository.save(limit("2000.00", at(2026, 1, 10), ExpenseCategory.PRODUCT));

            var result = limitRepository.findLatestValidLimit(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), at(2026, 1, 15));

            assertThat(result).isPresent();
            assertThat(result.get().getLimitSum()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("Limit change within month: transaction before change sees the OLD limit")
        void shouldSeeOldLimitBeforeChangeWithinMonth() {
            limitRepository.save(limit("1000.00", at(2026, 1, 1), ExpenseCategory.PRODUCT));
            limitRepository.save(limit("2000.00", at(2026, 1, 10), ExpenseCategory.PRODUCT));

            var result = limitRepository.findLatestValidLimit(
                    ACCOUNT, ExpenseCategory.PRODUCT.name(), at(2026, 1, 5));

            assertThat(result).isPresent();
            assertThat(result.get().getLimitSum()).isEqualByComparingTo("1000.00");
        }
    }

    @Nested
    @DisplayName("Find All Limits By Account Tests")
    class FindAllByAccountTests {

        @Test
        @DisplayName("Should return all client limits ordered by date descending")
        void shouldReturnAllLimitsOrderedByDateDesc() {
            limitRepository.save(limit("1000.00", at(2026, 1, 1), ExpenseCategory.PRODUCT));
            limitRepository.save(limit("1500.00", at(2026, 2, 1), ExpenseCategory.PRODUCT));
            limitRepository.save(limit("2000.00", at(2026, 1, 1), ExpenseCategory.SERVICE));

            List<Limit> result = limitRepository.findAllByAccountFromOrderByLimitDatetimeDesc(ACCOUNT);

            assertThat(result)
                    .hasSize(3)
                    .extracting(Limit::getLimitSum)
                    .containsExactly(
                            new BigDecimal("1500.00"),
                            new BigDecimal("1000.00"),
                            new BigDecimal("2000.00"));
        }

        @Test
        @DisplayName("Should return empty list when the client has no limits")
        void shouldReturnEmptyListForUnknownAccount() {
            List<Limit> result = limitRepository
                    .findAllByAccountFromOrderByLimitDatetimeDesc("9999999999");

            assertThat(result).isEmpty();
        }
    }
}