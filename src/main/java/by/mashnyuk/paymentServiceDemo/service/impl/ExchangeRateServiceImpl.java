package by.mashnyuk.paymentServiceDemo.service.impl;

import by.mashnyuk.paymentServiceDemo.exception.ExchangeRateClientException;
import by.mashnyuk.paymentServiceDemo.exception.ExchangeRateUnavailableException;
import by.mashnyuk.paymentServiceDemo.model.ExchangeRate;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExchangeRateApiResponse;
import by.mashnyuk.paymentServiceDemo.repository.ExchangeRateRepository;
import by.mashnyuk.paymentServiceDemo.service.ExchangeRateExternalService;
import by.mashnyuk.paymentServiceDemo.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateExternalService externalService;

    @Override
    @Cacheable(
            cacheNames = "rates",
            key = "#date + '_' + #currencyPair",
            unless = "#result == null"
    )
    public BigDecimal getRate(
            LocalDate date,
            String currencyPair
    ) {
        Optional<ExchangeRate> storedRate =
                exchangeRateRepository.findByCurrencyPairAndRateDate(
                        currencyPair,
                        date
                );

        if (storedRate.isPresent()) {
            return storedRate.get().getCloseRate() != null
                    ? storedRate.get().getCloseRate()
                    : storedRate.get().getPreviousCloseRate();
        }

        return resolveMissingRate(date, currencyPair);
    }

    private BigDecimal resolveMissingRate(
            LocalDate date,
            String currencyPair
    ) {
        try {
            ExchangeRateApiResponse response =
                    externalService.getRate(
                            currencyPair,
                            date
                    );

            ExchangeRateApiResponse.Value current =
                    extractCurrentValue(
                            response,
                            date
                    );

            BigDecimal closeRate = current.close();

            BigDecimal previousCloseRate =
                    extractPreviousClose(
                            response,
                            current,
                            date,
                            currencyPair
                    );

            saveExchangeRate(
                    date,
                    currencyPair,
                    closeRate,
                    previousCloseRate
            );

            return closeRate != null ? closeRate : previousCloseRate;

        } catch (ExchangeRateClientException
                 | ExchangeRateUnavailableException exception) {

            return findPreviousClose(
                    date,
                    currencyPair
            );
        }
    }

    private ExchangeRateApiResponse.Value extractCurrentValue(
            ExchangeRateApiResponse response,
            LocalDate requestedDate
    ) {
        if (response == null
                || response.values() == null
                || response.values().isEmpty()) {

            throw new ExchangeRateUnavailableException(
                    "Exchange rate API returned no values"
            );
        }

        return response.values()
                .stream()
                .filter(value -> isDate(
                        value.datetime(),
                        requestedDate
                ))
                .findFirst()
                .filter(value -> value.close() != null || value.previousClose() != null)
                .orElseThrow(() ->
                        new ExchangeRateUnavailableException(
                                "Exchange rate is not available for "
                                        + requestedDate
                        )
                );
    }

    private BigDecimal extractPreviousClose(
            ExchangeRateApiResponse response,
            ExchangeRateApiResponse.Value current,
            LocalDate requestedDate,
            String currencyPair
    ) {
        if (response.values() != null) {
            Optional<BigDecimal> previousFromResponse = response.values()
                    .stream()
                    .filter(value ->
                            (value.close() != null || value.previousClose() != null)
                                    && isBefore(
                                    value.datetime(),
                                    requestedDate
                            )
                    )
                    .findFirst()
                    .map(value -> value.close() != null ? value.close() : value.previousClose());

            if (previousFromResponse.isPresent()) {
                return previousFromResponse.get();
            }
        }

        if (current.previousClose() != null) {
            return current.previousClose();
        }

        return exchangeRateRepository.findLatestRateBefore(currencyPair, requestedDate)
                .map(rate -> rate.getCloseRate() != null ? rate.getCloseRate() : rate.getPreviousCloseRate())
                .orElse(current.close());
    }

    private boolean isDate(
            String value,
            LocalDate expectedDate
    ) {
        try {
            return parseDate(value).equals(expectedDate);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isBefore(
            String value,
            LocalDate date
    ) {
        try {
            LocalDate parsed = parseDate(value);
            return parsed != null && parsed.isBefore(date);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null) return null;
        try {
            if (value.contains(" ")) {
                value = value.split(" ")[0];
            }
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveExchangeRate(
            LocalDate date,
            String currencyPair,
            BigDecimal closeRate,
            BigDecimal previousCloseRate
    ) {
        ExchangeRate exchangeRate =
                ExchangeRate.builder()
                        .rateDate(date)
                        .currencyPair(currencyPair)
                        .closeRate(closeRate)
                        .previousCloseRate(previousCloseRate)
                        .build();

        exchangeRateRepository.save(exchangeRate);
    }

    private BigDecimal findPreviousClose(
            LocalDate date,
            String currencyPair
    ) {
        ExchangeRate previousRate =
                exchangeRateRepository
                        .findLatestRateBefore(
                                currencyPair,
                                date
                        )
                        .orElseThrow(() ->
                                new ExchangeRateUnavailableException(
                                        "No exchange rate available for "
                                                + currencyPair
                                                + " at "
                                                + date
                                )
                        );

        if (previousRate.getCloseRate() != null) {
            return previousRate.getCloseRate();
        }

        return previousRate.getPreviousCloseRate();
    }
}