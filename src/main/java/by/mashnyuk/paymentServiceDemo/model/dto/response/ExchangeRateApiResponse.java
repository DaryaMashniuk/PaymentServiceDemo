package by.mashnyuk.paymentServiceDemo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateApiResponse(
        String status,
        String symbol,
        List<Value> values,
        String message
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            String datetime,
            BigDecimal close,

            @JsonProperty("previous_close")
            BigDecimal previousClose
    ) {
    }
}
