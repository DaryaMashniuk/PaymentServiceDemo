package by.mashnyuk.paymentServiceDemo.model.dto.response;

import by.mashnyuk.paymentServiceDemo.model.Currency;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record ExceededTransactionResponseDto(

        @JsonProperty("account_from")
        String accountFrom,

        @JsonProperty("account_to")
        String accountTo,

        @JsonProperty("currency_shortname")
        Currency currencyShortname,

        @JsonProperty("sum")
        BigDecimal sum,

        @JsonProperty("expense_category")
        ExpenseCategory expenseCategory,

        @JsonProperty("datetime")
        OffsetDateTime datetime,

        @JsonProperty("limit_sum")
        BigDecimal limitSum,

        @JsonProperty("limit_datetime")
        OffsetDateTime limitDatetime,

        @JsonProperty("limit_currency_shortname")
        String limitCurrencyShortname

) {}