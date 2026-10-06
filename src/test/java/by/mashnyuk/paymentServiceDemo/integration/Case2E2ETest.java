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
class Case2E2ETest extends AbstractIntegrationTest {

    private static final String ACCOUNT = "0000000555";
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
                                    { "datetime": "2022-02-01", "close": "1.00000000", "previous_close": "1.00000000" }
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
    @DisplayName("E2E: Replicate Case 2 from specification via HTTP API (Limit lowered mid-month)")
    void shouldReplicateCase2Successfully() {

        mockClockTime("2022-02-01T00:00:00Z");
        setLimit("1000.00", ExpenseCategory.PRODUCT);

        sendTransaction("500.00", "2022-02-02T12:00:00Z");

        sendTransaction("100.00", "2022-02-03T12:00:00Z");

        mockClockTime("2022-02-10T00:00:00Z");
        setLimit("400.00", ExpenseCategory.PRODUCT);

        sendTransaction("100.00", "2022-02-11T12:00:00Z");

        sendTransaction("100.00", "2022-02-12T12:00:00Z");

        List<ExceededTransactionResponseDto> exceeded = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transactions/exceeded")
                        .queryParam("account", ACCOUNT)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        assertThat(exceeded).isNotNull().hasSize(2);

        ExceededTransactionResponseDto txFeb11 = exceeded.get(0);
        assertThat(txFeb11.getSum()).isEqualByComparingTo("100.00");
        assertThat(txFeb11.getDatetime()).isEqualTo(OffsetDateTime.parse("2022-02-11T12:00:00Z"));
        assertThat(txFeb11.getLimitSum()).isEqualByComparingTo("400.00");
        assertThat(txFeb11.getLimitDatetime()).isEqualTo(OffsetDateTime.parse("2022-02-10T00:00:00Z"));

        ExceededTransactionResponseDto txFeb12 = exceeded.get(1);
        assertThat(txFeb12.getSum()).isEqualByComparingTo("100.00");
        assertThat(txFeb12.getDatetime()).isEqualTo(OffsetDateTime.parse("2022-02-12T12:00:00Z"));
        assertThat(txFeb12.getLimitSum()).isEqualByComparingTo("400.00");
        assertThat(txFeb12.getLimitDatetime()).isEqualTo(OffsetDateTime.parse("2022-02-10T00:00:00Z"));
    }

    private void setLimit(String limitSum, ExpenseCategory category) {
        LimitRequestDto dto = LimitRequestDto.builder()
                .accountFrom(ACCOUNT)
                .limitSum(new BigDecimal(limitSum))
                .expenseCategory(category)
                .build();

        restClient.post()
                .uri("/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
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