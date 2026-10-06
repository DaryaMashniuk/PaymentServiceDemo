package by.mashnyuk.paymentServiceDemo.model.dto.request;

import by.mashnyuk.paymentServiceDemo.model.Currency;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
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
public class TransactionRequestDto {

    @NotNull(message = "account_from is required")
    @Pattern(regexp = "^\\d{10}$", message = "account_from must be exactly 10 digits")
    @JsonProperty("account_from")
    private String accountFrom;

    @NotNull(message = "account_to is required")
    @Pattern(regexp = "^\\d{10}$", message = "account_to must be exactly 10 digits")
    @JsonProperty("account_to")
    private String accountTo;

    @NotNull(message = "currency_shortname is required")
    @JsonProperty("currency_shortname")
    private Currency currencyShortname;

    @NotNull(message = "sum is required")
    @Positive(message = "sum must be strictly positive")
    @Digits(integer = 16, fraction = 2, message = "sum can have up to 16 integer digits and 2 decimal places")
    @JsonProperty("sum")
    private BigDecimal sum;

    @NotNull(message = "expense_category is required")
    @JsonProperty("expense_category")
    private ExpenseCategory expenseCategory;

    @NotNull(message = "datetime is required")
    @JsonProperty("datetime")
    private OffsetDateTime datetime;
}
