package by.mashnyuk.paymentServiceDemo.client;

import by.mashnyuk.paymentServiceDemo.exception.ExchangeRateClientException;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExchangeRateApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Set;

@Component
public class TwelveDataExchangeRateClient implements ExchangeRateClient {

    private static final Set<HttpStatusCode> RETRYABLE_STATUSES = Set.of(
            HttpStatus.TOO_MANY_REQUESTS,
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.GATEWAY_TIMEOUT
    );

    private final RestClient exchangeRateRestClient;
    private final String apiKey;

    public TwelveDataExchangeRateClient(
            RestClient exchangeRateRestClient,
            @Value("${exchange-rate.client.api-key}") String apiKey
    ) {
        this.exchangeRateRestClient = exchangeRateRestClient;
        this.apiKey = apiKey;
    }

    @Override
    public ExchangeRateApiResponse getRate(String pair, LocalDate date) {
        try {
            ExchangeRateApiResponse response = exchangeRateRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/time_series")
                            .queryParam("symbol", pair)
                            .queryParam("interval", "1day")
                            .queryParam("start_date", date.toString())
                            .queryParam("end_date", date.toString())
                            .queryParam("date", date.toString())
                            .queryParam("outputsize", 2)
                            .queryParam("apikey", apiKey)
                            .build()
                    )
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                        boolean isRetryable = RETRYABLE_STATUSES.contains(httpResponse.getStatusCode());
                        throw new ExchangeRateClientException(
                                "Exchange rate API error status: " + httpResponse.getStatusCode(),
                                isRetryable
                        );
                    })
                    .body(ExchangeRateApiResponse.class);

            if (response == null) {
                throw new ExchangeRateClientException("Empty response from exchange rate API");
            }

            if ("error".equalsIgnoreCase(response.status())) {
                throw new ExchangeRateClientException(
                        response.message() != null
                                ? response.message()
                                : "Exchange rate API returned an error"
                );
            }

            return response;
        } catch (ExchangeRateClientException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ExchangeRateClientException("Failed to call exchange rate service", e,true);
        }
    }
}