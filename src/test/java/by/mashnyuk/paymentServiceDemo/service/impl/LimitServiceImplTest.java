package by.mashnyuk.paymentServiceDemo.service.impl;

import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Limit;
import by.mashnyuk.paymentServiceDemo.repository.LimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitServiceImplTest {

    private static final String ACCOUNT = "0000000123";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private LimitRepository limitRepository;

    private Clock clock;
    private LimitServiceImpl limitService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        limitService = new LimitServiceImpl(limitRepository, clock);
    }

    @Nested
    @DisplayName("setLimit() Tests")
    class SetLimitTests {

        @Test
        @DisplayName("Should successfully set limit with clock timestamp and correct scale")
        void shouldSetLimitSuccessfully() {
            BigDecimal inputSum = new BigDecimal("1500.5");
            OffsetDateTime expectedTime = OffsetDateTime.now(clock);

            when(limitRepository.save(any(Limit.class))).thenAnswer(i -> i.getArgument(0));

            Limit result = limitService.setLimit(ACCOUNT, inputSum, ExpenseCategory.PRODUCT);

            ArgumentCaptor<Limit> captor = ArgumentCaptor.forClass(Limit.class);
            verify(limitRepository).save(captor.capture());

            Limit saved = captor.getValue();
            assertThat(saved.getAccountFrom()).isEqualTo(ACCOUNT);
            assertThat(saved.getLimitSum()).isEqualByComparingTo("1500.50");
            assertThat(saved.getLimitSum().scale()).isEqualTo(2);
            assertThat(saved.getLimitDatetime()).isEqualTo(expectedTime);
            assertThat(saved.getLimitCurrencyShortname()).isEqualTo("USD");
            assertThat(saved.getExpenseCategory()).isEqualTo(ExpenseCategory.PRODUCT);

            assertThat(result).isEqualTo(saved);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Should throw IllegalArgumentException when account is blank or null")
        void shouldThrowWhenAccountIsBlank(String invalidAccount) {
            assertThatThrownBy(() -> limitService.setLimit(invalidAccount, BigDecimal.TEN, ExpenseCategory.PRODUCT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Account must not be blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when limit sum is null")
        void shouldThrowWhenSumIsNull() {
            assertThatThrownBy(() -> limitService.setLimit(ACCOUNT, null, ExpenseCategory.PRODUCT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Limit must be greater than or equal to zero");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when limit sum is negative")
        void shouldThrowWhenSumIsNegative() {
            BigDecimal negativeSum = new BigDecimal("-0.01");
            assertThatThrownBy(() -> limitService.setLimit(ACCOUNT, negativeSum, ExpenseCategory.PRODUCT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Limit must be greater than or equal to zero");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when category is null")
        void shouldThrowWhenCategoryIsNull() {
            assertThatThrownBy(() -> limitService.setLimit(ACCOUNT, BigDecimal.TEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Expense category must not be null");
        }
    }

    @Nested
    @DisplayName("getActualLimit() Tests")
    class GetActualLimitTests {

        @Test
        @DisplayName("Should return existing historical limit when present in DB")
        void shouldReturnExistingLimit() {
            OffsetDateTime txTime = OffsetDateTime.parse("2026-01-10T12:00:00Z");
            Limit existing = Limit.builder()
                    .id(1L)
                    .accountFrom(ACCOUNT)
                    .limitSum(new BigDecimal("2000.00"))
                    .limitDatetime(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
                    .limitCurrencyShortname("USD")
                    .expenseCategory(ExpenseCategory.SERVICE)
                    .build();

            when(limitRepository.findLatestValidLimit(eq(ACCOUNT), eq("SERVICE"), eq(txTime)))
                    .thenReturn(Optional.of(existing));

            Limit actual = limitService.getActualLimit(ACCOUNT, ExpenseCategory.SERVICE, txTime);

            assertThat(actual).isEqualTo(existing);
            assertThat(actual.getLimitSum()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("Boundary: Should return default 1000.00 USD limit when no limit was explicitly set")
        void shouldReturnDefaultLimitWhenNotFound() {
            OffsetDateTime txTime = OffsetDateTime.parse("2026-01-10T12:00:00Z");

            when(limitRepository.findLatestValidLimit(eq(ACCOUNT), eq("PRODUCT"), eq(txTime)))
                    .thenReturn(Optional.empty());

            Limit actual = limitService.getActualLimit(ACCOUNT, ExpenseCategory.PRODUCT, txTime);

            assertThat(actual.getId()).isNull();
            assertThat(actual.getAccountFrom()).isEqualTo(ACCOUNT);
            assertThat(actual.getLimitSum()).isEqualByComparingTo("1000.00");
            assertThat(actual.getLimitCurrencyShortname()).isEqualTo("USD");
            assertThat(actual.getExpenseCategory()).isEqualTo(ExpenseCategory.PRODUCT);
            assertThat(actual.getLimitDatetime()).isEqualTo(txTime);

            verify(limitRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when datetime is null")
        void shouldThrowWhenDatetimeIsNull() {
            assertThatThrownBy(() -> limitService.getActualLimit(ACCOUNT, ExpenseCategory.PRODUCT, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Datetime must not be null");
        }
    }

    @Nested
    @DisplayName("getAllLimits() Tests")
    class GetAllLimitsTests {

        @Test
        @DisplayName("Should return all limits ordered by datetime desc")
        void shouldReturnAllLimits() {
            List<Limit> limits = List.of(
                    Limit.builder().id(2L).limitSum(new BigDecimal("2000.00")).build(),
                    Limit.builder().id(1L).limitSum(new BigDecimal("1000.00")).build()
            );

            when(limitRepository.findAllByAccountFromOrderByLimitDatetimeDesc(ACCOUNT))
                    .thenReturn(limits);

            List<Limit> result = limitService.getAllLimits(ACCOUNT);

            assertThat(result).hasSize(2).isEqualTo(limits);
            verify(limitRepository).findAllByAccountFromOrderByLimitDatetimeDesc(ACCOUNT);
        }
    }
}