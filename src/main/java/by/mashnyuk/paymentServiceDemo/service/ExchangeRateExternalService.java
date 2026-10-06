package by.mashnyuk.paymentServiceDemo.service;

import by.mashnyuk.paymentServiceDemo.client.ExchangeRateClient;
import by.mashnyuk.paymentServiceDemo.config.ExchangeRateMetrics;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExchangeRateApiResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExchangeRateExternalService {

    private static final String CIRCUIT_BREAKER_NAME = "exchangeRateApi";

    private final ExchangeRateClient exchangeRateClient;
    private final ExchangeRateMetrics metrics;

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ExchangeRateApiResponse getRate(String pair, LocalDate date) {

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

        return circuitBreaker.executeSupplier(() -> {
            Timer.Sample sample = metrics.startTimer();

            try {
                ExchangeRateApiResponse response = exchangeRateClient.getRate(pair, date);
                metrics.recordSuccess(pair, sample);
                return response;
            } catch (RuntimeException exception) {
                metrics.recordError(pair, sample);
                throw exception;
            }
        });
    }
}