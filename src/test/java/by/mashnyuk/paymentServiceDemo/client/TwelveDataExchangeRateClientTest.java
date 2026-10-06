package by.mashnyuk.paymentServiceDemo.client;

import by.mashnyuk.paymentServiceDemo.exception.ExchangeRateClientException;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExchangeRateApiResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TwelveDataExchangeRateClientTest {

    private static final String API_KEY = "test-api-key";

    private static WireMockServer wireMock;

    private TwelveDataExchangeRateClient client;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(0);
        wireMock.start();
        configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @BeforeEach
    void setUp() {
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .build();

        client = new TwelveDataExchangeRateClient(restClient, API_KEY);
        wireMock.resetAll();
    }

    @Test
    @DisplayName("Should parse successful TwelveData response and build request with date")
    void shouldParseSuccessfulResponse() {
        stubFor(
                get(urlPathEqualTo("/time_series"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                                {
                                                  "status": "ok",
                                                  "symbol": "KZT/USD",
                                                  "values": [
                                                    {
                                                      "datetime": "2026-10-05",
                                                      "close": "0.002100",
                                                      "previous_close": "0.002050"
                                                    }
                                                  ]
                                                }
                                                """)
                        )
        );

        ExchangeRateApiResponse response = client.getRate(
                "KZT/USD",
                LocalDate.of(2026, 10, 5)
        );

        assertThat(response.status()).isEqualTo("ok");
        assertThat(response.symbol()).isEqualTo("KZT/USD");
        assertThat(response.values()).hasSize(1);
        assertThat(response.values().getFirst().close()).isEqualByComparingTo("0.002100");
        assertThat(response.values().getFirst().previousClose()).isEqualByComparingTo("0.002050");

        verify(
                getRequestedFor(urlPathEqualTo("/time_series"))
                        .withQueryParam("symbol", equalTo("KZT/USD"))
                        .withQueryParam("interval", equalTo("1day"))
                        .withQueryParam("apikey", equalTo(API_KEY))
                        .withQueryParam("date", equalTo("2026-10-05"))
        );
    }

    @Test
    @DisplayName("Should reject HTTP 400")
    void shouldRejectBadRequest() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> client.getRate("KZT/USD", LocalDate.of(2026, 10, 5)))
                .isInstanceOf(ExchangeRateClientException.class);

        verify(1, getRequestedFor(anyUrl()));
    }

    @Test
    @DisplayName("Should reject unauthorized response")
    void shouldRejectUnauthorized() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> client.getRate("KZT/USD", LocalDate.of(2026, 10, 5)))
                .isInstanceOf(ExchangeRateClientException.class);
    }

    @Test
    @DisplayName("Should reject not found response")
    void shouldRejectNotFound() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.getRate("KZT/USD", LocalDate.of(2026, 10, 5)))
                .isInstanceOf(ExchangeRateClientException.class);
    }

    @Test
    @DisplayName("Should classify 429 as retryable")
    void shouldClassifyTooManyRequestsAsRetryable() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(429)));

        ExchangeRateClientException exception = catchException(() ->
                client.getRate("KZT/USD", LocalDate.of(2026, 10, 5))
        );

        assertThat(exception.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("Should classify 500 as retryable")
    void shouldClassifyServerErrorAsRetryable() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

        ExchangeRateClientException exception = catchException(() ->
                client.getRate("KZT/USD", LocalDate.of(2026, 10, 5))
        );

        assertThat(exception.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("Should classify 502/503/504 as retryable")
    void shouldClassifyGatewayErrorsAsRetryable() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(504)));

        ExchangeRateClientException exception = catchException(() ->
                client.getRate("KZT/USD", LocalDate.of(2026, 10, 5))
        );

        assertThat(exception.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("Should reject API-level error response")
    void shouldRejectApiLevelError() {
        stubFor(
                get(anyUrl())
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                                {
                                                  "status": "error",
                                                  "message": "Invalid API key"
                                                }
                                                """)
                        )
        );

        assertThatThrownBy(() -> client.getRate("KZT/USD", LocalDate.of(2026, 10, 5)))
                .isInstanceOf(ExchangeRateClientException.class);
    }

    @Test
    @DisplayName("Should reject malformed JSON")
    void shouldRejectMalformedJson() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{invalid-json")));

        assertThatThrownBy(() -> client.getRate("KZT/USD", LocalDate.of(2026, 10, 5)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should reject empty response")
    void shouldRejectEmptyResponse() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody("")));

        assertThatThrownBy(() -> client.getRate("KZT/USD", LocalDate.of(2026, 10, 5)))
                .isInstanceOf(RuntimeException.class);
    }

    private ExchangeRateClientException catchException(Runnable action) {
        try {
            action.run();
        } catch (ExchangeRateClientException exception) {
            return exception;
        }
        throw new AssertionError("Expected ExchangeRateClientException");
    }
}