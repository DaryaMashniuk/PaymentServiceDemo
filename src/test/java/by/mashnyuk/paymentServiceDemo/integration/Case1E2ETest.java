package by.mashnyuk.paymentServiceDemo.integration;

import by.mashnyuk.paymentServiceDemo.AbstractIntegrationTest;
import by.mashnyuk.paymentServiceDemo.model.Currency;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.dto.request.LimitRequestDto;
import by.mashnyuk.paymentServiceDemo.model.dto.request.TransactionRequestDto;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExceededTransactionResponseDto;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WireMockTest(httpPort = 8089)
class Case1E2ETest extends AbstractIntegrationTest {

    private static final String ACCOUNT = "0000000123";
    private static final String COUNTERPARTY = "9999999999";

    @LocalServerPort
    private int port;

    @MockitoBean
    private Clock clock;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1")
                .build();

        stubFor(WireMock.get(urlPathEqualTo("/time_series"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "status": "ok",
                                  "values": [
                                    { "datetime": "2022-01-01", "close": "1.00000000", "previous_close": "1.00000000" }
                                  ]
                                }
                                """)
                        .withStatus(200)));
    }

    private void mockClockTime(String isoInstant) {
        Instant instant = Instant.parse(isoInstant);
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("E2E: Replicate Case 1 from specification via HTTP API")
    void shouldReplicateCase1Successfully() {

        mockClockTime("2022-01-01T00:00:00Z");
        LimitRequestDto limit1 = LimitRequestDto.builder()
                .accountFrom(ACCOUNT)
                .limitSum(new BigDecimal("1000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .build();

        restClient.post()
                .uri("/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .body(limit1)
                .retrieve()
                .toBodilessEntity();

        sendTransaction("500.00", "2022-01-02T12:00:00Z");

        sendTransaction("600.00", "2022-01-03T12:00:00Z");

        mockClockTime("2022-01-10T00:00:00Z");
        LimitRequestDto limit2 = LimitRequestDto.builder()
                .accountFrom(ACCOUNT)
                .limitSum(new BigDecimal("2000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .build();

        restClient.post()
                .uri("/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .body(limit2)
                .retrieve()
                .toBodilessEntity();

        sendTransaction("100.00", "2022-01-11T12:00:00Z");

        sendTransaction("700.00", "2022-01-12T12:00:00Z");

        sendTransaction("100.00", "2022-01-13T10:00:00Z");

        sendTransaction("100.00", "2022-01-13T15:00:00Z");

        List<ExceededTransactionResponseDto> exceeded = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transactions/exceeded")
                        .queryParam("account", ACCOUNT)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        assertThat(exceeded).isNotNull().hasSize(2);

        ExceededTransactionResponseDto txJan3 = exceeded.get(0);
        assertThat(txJan3.getSum()).isEqualByComparingTo("600.00");
        assertThat(txJan3.getDatetime()).isEqualTo(OffsetDateTime.parse("2022-01-03T12:00:00Z"));
        assertThat(txJan3.getLimitSum()).isEqualByComparingTo("1000.00");
        assertThat(txJan3.getLimitDatetime()).isEqualTo(OffsetDateTime.parse("2022-01-01T00:00:00Z"));

        ExceededTransactionResponseDto txJan13 = exceeded.get(1);
        assertThat(txJan13.getSum()).isEqualByComparingTo("100.00");
        assertThat(txJan13.getDatetime()).isEqualTo(OffsetDateTime.parse("2022-01-13T15:00:00Z"));
        assertThat(txJan13.getLimitSum()).isEqualByComparingTo("2000.00");
        assertThat(txJan13.getLimitDatetime()).isEqualTo(OffsetDateTime.parse("2022-01-10T00:00:00Z"));
    }

    private void sendTransaction(String sum, String isoDatetime) {
        TransactionRequestDto dto = TransactionRequestDto.builder()
                .accountFrom(ACCOUNT)
                .accountTo(COUNTERPARTY)
                .currencyShortname(Currency.USD)
                .sum(new BigDecimal(sum))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(OffsetDateTime.parse(isoDatetime))
                .build();

        restClient.post()
                .uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }
}