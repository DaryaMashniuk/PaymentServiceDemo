package by.mashnyuk.paymentServiceDemo.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service for retrieving and caching foreign exchange (forex) rates.
 * <p>
 * Implements multi-tier exchange rate resolution (L1 Redis Cache &rarr;
 * L2 PostgreSQL Cache &rarr; External market data provider TwelveData &rarr;
 * Fallback to {@code previous_close} rate).
 */
public interface ExchangeRateService {

    /**
     * Retrieves the close exchange rate for a given currency pair on a specified date.
     *
     * @param date         the quotation date
     * @param currencyPair the currency pair in ISO 4217 format (e.g., {@code "KZT/USD"}, {@code "RUB/USD"})
     * @return the closing exchange rate as a {@link BigDecimal}
     * @throws IllegalStateException if the exchange rate is unavailable from external providers and local cache
     */
    BigDecimal getRate(LocalDate date, String currencyPair);
}