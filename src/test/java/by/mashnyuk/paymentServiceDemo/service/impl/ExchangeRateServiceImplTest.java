package by.mashnyuk.paymentServiceDemo.service.impl;

import by.mashnyuk.paymentServiceDemo.exception.ExchangeRateClientException;
import by.mashnyuk.paymentServiceDemo.exception.ExchangeRateUnavailableException;
import by.mashnyuk.paymentServiceDemo.model.ExchangeRate;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExchangeRateApiResponse;
import by.mashnyuk.paymentServiceDemo.repository.ExchangeRateRepository;
import by.mashnyuk.paymentServiceDemo.service.ExchangeRateExternalService;
import by.mashnyuk.paymentServiceDemo.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ExchangeRateServiceImplTest.TestConfig.class)
class ExchangeRateServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);
    private static final String PAIR = "KZT/USD";

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("rates");
        }

        @Bean
        public ExchangeRateService exchangeRateService(
                ExchangeRateRepository repository,
                ExchangeRateExternalService externalService
        ) {
            return new ExchangeRateServiceImpl(repository, externalService);
        }
    }

    @MockitoBean
    private ExchangeRateRepository exchangeRateRepository;

    @MockitoBean
    private ExchangeRateExternalService externalService;

    @Autowired
    private ExchangeRateService service;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        if (cacheManager.getCache("rates") != null) {
            cacheManager.getCache("rates").clear();
        }
        reset(exchangeRateRepository, externalService);
    }

    @Test
    @DisplayName("Should return rate from PostgreSQL without calling external API")
    void shouldReturnRateFromDatabase() {
        ExchangeRate rate = exchangeRate(DATE, PAIR, "0.002100", "0.002050");

        when(exchangeRateRepository.findByCurrencyPairAndRateDate(PAIR, DATE))
                .thenReturn(Optional.of(rate));

        BigDecimal result = service.getRate(DATE, PAIR);

        assertThat(result).isEqualByComparingTo("0.002100");
        verifyNoInteractions(externalService);
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should request external API when database has no rate")
    void shouldRequestExternalApiWhenRateIsMissing() {
        when(exchangeRateRepository.findByCurrencyPairAndRateDate(PAIR, DATE))
                .thenReturn(Optional.empty());

        when(externalService.getRate(PAIR, DATE))
                .thenReturn(response(PAIR, "2026-10-05", "0.002100", "0.002050"));

        BigDecimal result = service.getRate(DATE, PAIR);

        assertThat(result).isEqualByComparingTo("0.002100");
        verify(externalService).getRate(PAIR, DATE);
        verify(exchangeRateRepository).save(argThat(rate ->
                rate.getRateDate().equals(DATE)
                        && rate.getCurrencyPair().equals(PAIR)
                        && rate.getCloseRate().compareTo(new BigDecimal("0.002100")) == 0
                        && rate.getPreviousCloseRate().compareTo(new BigDecimal("0.002050")) == 0
        ));
    }

    @Test
    @DisplayName("Should use close rate of previous record when external API fails")
    void shouldUsePreviousCloseWhenExternalApiFails() {
        ExchangeRate previousRate = exchangeRate(LocalDate.of(2026, 10, 4), PAIR, "0.002100", "0.002050");

        when(exchangeRateRepository.findByCurrencyPairAndRateDate(PAIR, DATE))
                .thenReturn(Optional.empty());
        when(externalService.getRate(PAIR, DATE))
                .thenThrow(new ExchangeRateClientException("API unavailable", true));
        when(exchangeRateRepository.findLatestRateBefore(PAIR, DATE))
                .thenReturn(Optional.of(previousRate));

        BigDecimal result = service.getRate(DATE, PAIR);

        assertThat(result).isEqualByComparingTo("0.002100");
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should use previous close rate when close rate is null in repository")
    void shouldUsePreviousCloseWhenCloseRateIsNullInDb() {
        ExchangeRate previousRate = exchangeRate(LocalDate.of(2026, 10, 4), PAIR, null, "0.002050");

        when(exchangeRateRepository.findByCurrencyPairAndRateDate(PAIR, DATE))
                .thenReturn(Optional.empty());
        when(externalService.getRate(PAIR, DATE))
                .thenThrow(new ExchangeRateClientException("API unavailable", true));
        when(exchangeRateRepository.findLatestRateBefore(PAIR, DATE))
                .thenReturn(Optional.of(previousRate));

        BigDecimal result = service.getRate(DATE, PAIR);

        assertThat(result).isEqualByComparingTo("0.002050");
    }

    @Test
    @DisplayName("Should reject response for another date and fall back to database")
    void shouldRejectResponseForAnotherDate() {
        when(exchangeRateRepository.findByCurrencyPairAndRateDate(PAIR, DATE))
                .thenReturn(Optional.empty());

        when(externalService.getRate(PAIR, DATE))
                .thenReturn(response(PAIR, "2026-10-04", "0.002000", "0.001950"));

        ExchangeRate previousRate = exchangeRate(LocalDate.of(2026, 10, 4), PAIR, "0.002000", "0.001950");
        when(exchangeRateRepository.findLatestRateBefore(PAIR, DATE))
                .thenReturn(Optional.of(previousRate));

        BigDecimal result = service.getRate(DATE, PAIR);

        assertThat(result).isEqualByComparingTo("0.002000");
    }

    @Test
    @DisplayName("Should throw exception when external API fails and no fallback exists")
    void shouldThrowExceptionWhenNoFallbackExists() {
        when(exchangeRateRepository.findByCurrencyPairAndRateDate(PAIR, DATE))
                .thenReturn(Optional.empty());
        when(externalService.getRate(PAIR, DATE))
                .thenThrow(new ExchangeRateClientException("API error", true));
        when(exchangeRateRepository.findLatestRateBefore(PAIR, DATE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRate(DATE, PAIR))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    // --- Дополнительные тесты ---

    @Test
    @DisplayName("Should return cached value on second call without querying repository or API")
    void shouldCacheGetRateResults() {
        ExchangeRate rate = exchangeRate(DATE, PAIR, "0.002100", "0.002050");
        when(exchangeRateRepository.findByCurrencyPairAndRateDate(PAIR, DATE))
                .thenReturn(Optional.of(rate));

        BigDecimal firstCall = service.getRate(DATE, PAIR);
        BigDecimal secondCall = service.getRate(DATE, PAIR);

        assertThat(firstCall).isEqualByComparingTo(secondCall);
        verify(exchangeRateRepository, times(1)).findByCurrencyPairAndRateDate(PAIR, DATE);
        verifyNoInteractions(externalService);
    }

    @Test
    @DisplayName("Should extract previous close from historical list in external API response when available")
    void shouldExtractPreviousCloseFromExternalHistory() {
        when(exchangeRateRepository.findByCurrencyPairAndRateDate(PAIR, DATE))
                .thenReturn(Optional.empty());

        ExchangeRateApiResponse.Value currentVal = new ExchangeRateApiResponse.Value("2026-10-05", new BigDecimal("0.002100"), null);
        ExchangeRateApiResponse.Value historicalVal = new ExchangeRateApiResponse.Value("2026-10-04", new BigDecimal("0.002080"), null);
        ExchangeRateApiResponse apiResponse = new ExchangeRateApiResponse("ok", PAIR, List.of(currentVal, historicalVal), null);

        when(externalService.getRate(PAIR, DATE)).thenReturn(apiResponse);

        BigDecimal result = service.getRate(DATE, PAIR);

        assertThat(result).isEqualByComparingTo("0.002100");
        verify(exchangeRateRepository).save(argThat(saved ->
                saved.getCloseRate().compareTo(new BigDecimal("0.002100")) == 0
                        && saved.getPreviousCloseRate().compareTo(new BigDecimal("0.002080")) == 0
        ));
    }

    @Test
    @DisplayName("Should throw ExchangeRateUnavailableException when API response contains empty value list")
    void shouldThrowExceptionWhenApiResponseIsEmpty() {
        when(exchangeRateRepository.findByCurrencyPairAndRateDate(PAIR, DATE))
                .thenReturn(Optional.empty());
        when(externalService.getRate(PAIR, DATE))
                .thenReturn(new ExchangeRateApiResponse("ok", PAIR, List.of(), null));
        when(exchangeRateRepository.findLatestRateBefore(PAIR, DATE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRate(DATE, PAIR))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    private ExchangeRate exchangeRate(LocalDate date, String pair, String close, String prevClose) {
        return ExchangeRate.builder()
                .rateDate(date)
                .currencyPair(pair)
                .closeRate(close != null ? new BigDecimal(close) : null)
                .previousCloseRate(prevClose != null ? new BigDecimal(prevClose) : null)
                .build();
    }

    private ExchangeRateApiResponse response(String pair, String dateStr, String close, String prevClose) {
        ExchangeRateApiResponse.Value value = new ExchangeRateApiResponse.Value(
                dateStr,
                close != null ? new BigDecimal(close) : null,
                prevClose != null ? new BigDecimal(prevClose) : null
        );
        return new ExchangeRateApiResponse("ok", pair, List.of(value), null);
    }
}