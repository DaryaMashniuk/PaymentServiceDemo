package by.mashnyuk.paymentServiceDemo.model.dto.request;

import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitRequestDto {

    @NotNull(message = "account_from is required")
    @Pattern(regexp = "^\\d{10}$", message = "account_from must be exactly 10 digits")
    @JsonProperty("account_from")
    private String accountFrom;

    @NotNull(message = "limit_sum is required")
    @PositiveOrZero(message = "limit_sum must be greater than or equal to zero")
    @Digits(integer = 16, fraction = 2, message = "limit_sum can have up to 16 integer digits and 2 decimal places")
    @JsonProperty("limit_sum")
    private BigDecimal limitSum;

    @NotNull(message = "expense_category is required")
    @JsonProperty("expense_category")
    private ExpenseCategory expenseCategory;
}