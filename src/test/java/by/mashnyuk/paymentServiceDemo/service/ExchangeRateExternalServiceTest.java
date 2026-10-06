package by.mashnyuk.paymentServiceDemo.service;

import by.mashnyuk.paymentServiceDemo.client.ExchangeRateClient;
import by.mashnyuk.paymentServiceDemo.config.ExchangeRateMetrics;
import by.mashnyuk.paymentServiceDemo.exception.ExchangeRateClientException;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExchangeRateApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExchangeRateExternalServiceTest {

    private static final String PAIR = "USD/EUR";
    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);
    private static final String CIRCUIT_BREAKER_NAME = "exchangeRateApi";

    @Nested
    @DisplayName("Unit tests for Metrics")
    @ExtendWith(MockitoExtension.class)
    class UnitMetricsTests {

        @Mock
        private ExchangeRateClient exchangeRateClient;

        @Mock
        private ExchangeRateMetrics metrics;

        @Mock
        private Timer.Sample sample;

        @Mock
        private CircuitBreakerRegistry circuitBreakerRegistry;
        @Mock
        private CircuitBreaker circuitBreaker;

        private ExchangeRateExternalService externalService;

        @BeforeEach
        void setUp() {
            lenient().when(circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME)).thenReturn(circuitBreaker);

            lenient().when(circuitBreaker.executeSupplier(any())).thenAnswer(invocation -> {
                java.util.function.Supplier<?> supplier = invocation.getArgument(0);
                return supplier.get();
            });

            externalService = new ExchangeRateExternalService(exchangeRateClient, metrics, circuitBreakerRegistry);
        }

        @Test
        @DisplayName("Should execute request and record success metrics")
        void shouldExecuteRequestAndRecordMetrics() {
            ExchangeRateApiResponse expectedResponse = new ExchangeRateApiResponse("ok", PAIR, List.of(), null);

            when(metrics.startTimer()).thenReturn(sample);
            when(exchangeRateClient.getRate(PAIR, DATE)).thenReturn(expectedResponse);

            ExchangeRateApiResponse actualResponse = externalService.getRate(PAIR, DATE);

            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(metrics).startTimer();
            verify(metrics).recordSuccess(PAIR, sample);
            verify(metrics, never()).recordError(any(), any());
        }

        @Test
        @DisplayName("Should record error metrics and rethrow exception when client fails")
        void shouldRecordErrorMetricsAndRethrowWhenClientFails() {
            ExchangeRateClientException expectedException = new ExchangeRateClientException("API Error", true);

            when(metrics.startTimer()).thenReturn(sample);
            when(exchangeRateClient.getRate(PAIR, DATE)).thenThrow(expectedException);

            assertThatThrownBy(() -> externalService.getRate(PAIR, DATE))
                    .isInstanceOf(ExchangeRateClientException.class)
                    .hasMessage("API Error");

            verify(metrics).startTimer();
            verify(metrics).recordError(PAIR, sample);
            verify(metrics, never()).recordSuccess(any(), any());
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Aspect Tests")
    @ExtendWith(SpringExtension.class)
    @SpringBootTest
    class CircuitBreakerTests {

        @MockitoBean
        private ExchangeRateClient exchangeRateClient;

        @MockitoBean
        private ExchangeRateMetrics metrics;

        @Autowired
        private ExchangeRateExternalService externalService;

        @Autowired
        private CircuitBreakerRegistry circuitBreakerRegistry;

        private CircuitBreaker circuitBreaker;

        @BeforeEach
        void setUp() {
            circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
            circuitBreaker.reset();

            lenient().when(metrics.startTimer()).thenReturn(mock(Timer.Sample.class));
        }

        @Test
        @DisplayName("Circuit Breaker should open after minimum number of failed calls threshold")
        void shouldOpenCircuitBreakerWhenFailureThresholdReached() {
            when(exchangeRateClient.getRate(PAIR, DATE))
                    .thenThrow(new ExchangeRateClientException("API Error", true));

            int minimumNumberOfCalls = circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls();
            for (int i = 0; i < minimumNumberOfCalls; i++) {
                assertThatThrownBy(() -> externalService.getRate(PAIR, DATE))
                        .isInstanceOf(ExchangeRateClientException.class);
            }

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            assertThatThrownBy(() -> externalService.getRate(PAIR, DATE))
                    .isInstanceOf(CallNotPermittedException.class);

            verify(exchangeRateClient, times(minimumNumberOfCalls)).getRate(PAIR, DATE);
        }

        @Test
        @DisplayName("Circuit Breaker should remain CLOSED when calls are successful")
        void shouldRemainClosedWhenCallsAreSuccessful() {
            ExchangeRateApiResponse successResponse = new ExchangeRateApiResponse("ok", PAIR, List.of(), null);
            when(exchangeRateClient.getRate(PAIR, DATE)).thenReturn(successResponse);

            for (int i = 0; i < 5; i++) {
                externalService.getRate(PAIR, DATE);
            }

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            verify(exchangeRateClient, times(5)).getRate(PAIR, DATE);
        }

        @Test
        @DisplayName("Circuit Breaker should transition to HALF_OPEN and then back to CLOSED after successful trial calls")
        void shouldTransitionToHalfOpenAndResetToClosed() {
            circuitBreaker.transitionToOpenState();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            circuitBreaker.transitionToHalfOpenState();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

            ExchangeRateApiResponse successResponse = new ExchangeRateApiResponse("ok", PAIR, List.of(), null);
            when(exchangeRateClient.getRate(PAIR, DATE)).thenReturn(successResponse);

            int permittedNumberOfCalls = circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState();
            for (int i = 0; i < permittedNumberOfCalls; i++) {
                externalService.getRate(PAIR, DATE);
            }

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }
}