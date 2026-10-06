package by.mashnyuk.paymentServiceDemo.model.dto.response;

import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record LimitResponseDto(

        @JsonProperty("account_from")
        String accountFrom,

        @JsonProperty("limit_sum")
        BigDecimal limitSum,

        @JsonProperty("limit_datetime")
        OffsetDateTime limitDatetime,

        @JsonProperty("limit_currency_shortname")
        String limitCurrencyShortname,

        @JsonProperty("expense_category")
        ExpenseCategory expenseCategory

) {}