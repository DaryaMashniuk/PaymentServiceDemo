package by.mashnyuk.paymentServiceDemo.controller;

import by.mashnyuk.paymentServiceDemo.mapper.LimitMapper;
import by.mashnyuk.paymentServiceDemo.model.Currency;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Limit;

import by.mashnyuk.paymentServiceDemo.model.dto.request.LimitRequestDto;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExceededTransactionResponseDto;
import by.mashnyuk.paymentServiceDemo.model.dto.response.LimitResponseDto;
import by.mashnyuk.paymentServiceDemo.service.LimitService;
import by.mashnyuk.paymentServiceDemo.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @MockitoBean
    private LimitService limitService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private LimitMapper limitMapper;

    @Test
    @DisplayName("POST /api/v1/limits - Should create limit and return 201 Created")
    void shouldCreateLimitSuccessfully() throws Exception {
        LimitRequestDto request = LimitRequestDto.builder()
                .accountFrom("0000000123")
                .limitSum(new BigDecimal("1500.00"))
                .expenseCategory(ExpenseCategory.SERVICE)
                .build();

        LimitResponseDto responseDto = LimitResponseDto.builder()
                .accountFrom("0000000123")
                .limitSum(new BigDecimal("1500.00"))
                .limitCurrencyShortname("USD")
                .expenseCategory(ExpenseCategory.SERVICE)
                .limitDatetime(OffsetDateTime.parse("2026-01-15T10:00:00Z"))
                .build();

        when(limitService.setLimit(eq("0000000123"), eq(new BigDecimal("1500.00")), eq(ExpenseCategory.SERVICE)))
                .thenReturn(new Limit());
        when(limitMapper.toDto(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.account_from").value("0000000123"))
                .andExpect(jsonPath("$.limit_sum").value(1500.00))
                .andExpect(jsonPath("$.limit_currency_shortname").value("USD"));
    }

    @Test
    @DisplayName("POST /api/v1/limits - Should reject negative limit with 400")
    void shouldRejectNegativeLimit() throws Exception {
        String json = """
                {
                  "account_from": "0000000123",
                  "limit_sum": -10.00,
                  "expense_category": "SERVICE"
                }
                """;

        mockMvc.perform(post("/api/v1/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.limit_sum").value("limit_sum must be greater than or equal to zero"));

        verifyNoInteractions(limitService);
    }

    @Test
    @DisplayName("GET /api/v1/limits - Should return 200 with list of limits for valid account")
    void shouldReturnLimitsForValidAccount() throws Exception {
        Limit limit = Limit.builder()
                .accountFrom("0000000123")
                .limitSum(new BigDecimal("2000.00"))
                .build();

        LimitResponseDto dto = LimitResponseDto.builder()
                .accountFrom("0000000123")
                .limitSum(new BigDecimal("2000.00"))
                .build();

        when(limitService.getAllLimits("0000000123")).thenReturn(List.of(limit));
        when(limitMapper.toDto(limit)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/limits")
                        .param("account", "0000000123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].account_from").value("0000000123"))
                .andExpect(jsonPath("$[0].limit_sum").value(2000.00));
    }

    @Test
    @DisplayName("GET /api/v1/limits - Should reject invalid account parameter (not 10 digits) with 400")
    void shouldRejectInvalidAccountForLimits() throws Exception {
        mockMvc.perform(get("/api/v1/limits")
                        .param("account", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Constraint Violation"));

        verifyNoInteractions(limitService);
    }

    @Test
    @DisplayName("GET /api/v1/transactions/exceeded - Should return enriched transactions list")
    void shouldReturnExceededTransactions() throws Exception {
        ExceededTransactionResponseDto dto = ExceededTransactionResponseDto.builder()
                .accountFrom("0000000123")
                .accountTo("9999999999")
                .currencyShortname(Currency.KZT)
                .sum(new BigDecimal("50000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(OffsetDateTime.parse("2026-01-03T10:00:00Z"))
                .limitSum(new BigDecimal("1000.00"))
                .limitDatetime(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
                .limitCurrencyShortname("USD")
                .build();

        when(transactionService.getExceededTransactions("0000000123")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/transactions/exceeded")
                        .param("account", "0000000123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].account_from").value("0000000123"))
                .andExpect(jsonPath("$[0].limit_sum").value(1000.00))
                .andExpect(jsonPath("$[0].limit_datetime").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$[0].limit_currency_shortname").value("USD"));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/exceeded - Should reject invalid account parameter with 400")
    void shouldRejectInvalidAccountForExceededTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/exceeded")
                        .param("account", "not_10_digits"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Constraint Violation"));

        verifyNoInteractions(transactionService);
    }
}