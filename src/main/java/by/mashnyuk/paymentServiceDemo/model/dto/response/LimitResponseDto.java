package by.mashnyuk.paymentServiceDemo.model.dto.response;

import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitResponseDto {

    @JsonProperty("account_from")
    private String accountFrom;

    @JsonProperty("limit_sum")
    private BigDecimal limitSum;

    @JsonProperty("limit_datetime")
    private OffsetDateTime limitDatetime;

    @JsonProperty("limit_currency_shortname")
    private String limitCurrencyShortname;

    @JsonProperty("expense_category")
    private ExpenseCategory expenseCategory;
}
