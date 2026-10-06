package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.AbstractIntegrationTest;
import by.mashnyuk.paymentServiceDemo.model.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
class ExchangeRateRepositoryTest extends AbstractIntegrationTest {

    private static final String KZT_USD = "KZT/USD";
    private static final String RUB_USD = "RUB/USD";

    @Autowired
    private ExchangeRateRepository repository;

    private ExchangeRate rate(LocalDate date, String pair, String close, String prev) {
        return ExchangeRate.builder()
                .rateDate(date)
                .currencyPair(pair)
                .closeRate(new BigDecimal(close))
                .previousCloseRate(new BigDecimal(prev))
                .build();
    }

    @Nested
    @DisplayName("Find Rate By Currency Pair And Date Tests")
    class FindByCurrencyPairAndDateTests {

        @Test
        @DisplayName("Should find a rate by currency pair and date")
        void shouldFindRateByCurrencyPairAndDate() {
            LocalDate date = LocalDate.of(2026, 1, 15);
            repository.save(rate(date, KZT_USD, "500.25000000", "499.75000000"));

            var result = repository.findByCurrencyPairAndRateDate(KZT_USD, date);

            assertThat(result).isPresent();
            assertThat(result.get().getCloseRate()).isEqualByComparingTo("500.25000000");
            assertThat(result.get().getPreviousCloseRate()).isEqualByComparingTo("499.75000000");
        }

        @Test
        @DisplayName("Should return empty Optional when no rate exists for the given date")
        void shouldReturnEmptyWhenRateMissingForDate() {
            repository.save(rate(LocalDate.of(2026, 1, 15), KZT_USD, "500.00", "499.00"));

            var result = repository.findByCurrencyPairAndRateDate(
                    KZT_USD, LocalDate.of(2026, 1, 16));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should distinguish currency pairs: KZT/USD is not confused with RUB/USD")
        void shouldNotConfuseCurrencyPairs() {
            LocalDate date = LocalDate.of(2026, 1, 15);
            repository.save(rate(date, KZT_USD, "500.00", "499.00"));
            repository.save(rate(date, RUB_USD, "90.00", "89.50"));

            var kzt = repository.findByCurrencyPairAndRateDate(KZT_USD, date);
            var rub = repository.findByCurrencyPairAndRateDate(RUB_USD, date);

            assertThat(kzt).isPresent();
            assertThat(kzt.get().getCloseRate()).isEqualByComparingTo("500.00");
            assertThat(rub).isPresent();
            assertThat(rub.get().getCloseRate()).isEqualByComparingTo("90.00");
        }
    }

    @Nested
    @DisplayName("Find Latest Rate Tests (previous_close fallback)")
    class FindLatestRateTests {

        @Test
        @DisplayName("Should return the latest available rate when no rate exists for the requested date")
        void shouldReturnLatestRateBeforeDate() {
            repository.save(rate(LocalDate.of(2026, 1, 13), KZT_USD, "500.00", "499.00"));
            repository.save(rate(LocalDate.of(2026, 1, 12), KZT_USD, "498.00", "497.00"));

            var result = repository.findLatestRateBefore(KZT_USD, LocalDate.of(2026, 1, 15));

            assertThat(result).isPresent();
            assertThat(result.get().getCloseRate()).isEqualByComparingTo("500.00");
            assertThat(result.get().getRateDate()).isEqualTo(LocalDate.of(2026, 1, 13));
        }

        @Test
        @DisplayName("Should return empty Optional when no rates exist before the given date")
        void shouldReturnEmptyWhenNoRatesBeforeDate() {
            repository.save(rate(LocalDate.of(2026, 1, 20), KZT_USD, "500.00", "499.00"));

            var result = repository.findLatestRateBefore(KZT_USD, LocalDate.of(2026, 1, 15));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Boundary: should return the rate exactly on the given date")
        void shouldReturnRateOnExactDate() {
            LocalDate date = LocalDate.of(2026, 1, 15);
            repository.save(rate(date, KZT_USD, "500.00", "499.00"));

            var result = repository.findLatestRateBefore(KZT_USD, date);

            assertThat(result).isPresent();
            assertThat(result.get().getRateDate()).isEqualTo(date);
        }
    }
}