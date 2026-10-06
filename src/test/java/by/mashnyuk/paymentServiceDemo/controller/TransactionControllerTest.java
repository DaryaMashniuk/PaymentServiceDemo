package by.mashnyuk.paymentServiceDemo.controller;

import by.mashnyuk.paymentServiceDemo.mapper.TransactionMapper;
import by.mashnyuk.paymentServiceDemo.model.Currency;
import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Transaction;
import by.mashnyuk.paymentServiceDemo.model.dto.request.TransactionRequestDto;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private TransactionMapper transactionMapper;

    @Test
    @DisplayName("Should return 201 CREATED and pass correct entity to service when JSON is valid")
    void shouldReturnCreatedForValidTransaction() throws Exception {
        TransactionRequestDto request = TransactionRequestDto.builder()
                .accountFrom("0000000123")
                .accountTo("9999999999")
                .currencyShortname(Currency.KZT)
                .sum(new BigDecimal("10000.45"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(OffsetDateTime.parse("2026-01-30T00:00:00+06:00"))
                .build();

        Transaction entity = new Transaction();
        when(transactionMapper.toEntity(any(TransactionRequestDto.class))).thenReturn(entity);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(transactionMapper).toEntity(any(TransactionRequestDto.class));
        verify(transactionService).processTransaction(entity);
    }

    @Test
    @DisplayName("Should return 400 when account_from is not exactly 10 digits")
    void shouldReturnBadRequestWhenAccountFromInvalid() throws Exception {
        String json = """
                {
                  "account_from": "123",
                  "account_to": "9999999999",
                  "currency_shortname": "KZT",
                  "sum": 100.00,
                  "expense_category": "PRODUCT",
                  "datetime": "2026-01-30T00:00:00+06:00"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.account_from").value("account_from must be exactly 10 digits"));

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("Should return 400 when account_to is not exactly 10 digits")
    void shouldReturnBadRequestWhenAccountToInvalid() throws Exception {
        String json = """
                {
                  "account_from": "0000000123",
                  "account_to": "12345ABCDE",
                  "currency_shortname": "KZT",
                  "sum": 100.00,
                  "expense_category": "PRODUCT",
                  "datetime": "2026-01-30T00:00:00+06:00"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.account_to").value("account_to must be exactly 10 digits"));

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("Should return 400 when sum is missing")
    void shouldReturnBadRequestWhenSumMissing() throws Exception {
        String json = """
                {
                  "account_from": "0000000123",
                  "account_to": "9999999999",
                  "currency_shortname": "KZT",
                  "expense_category": "PRODUCT",
                  "datetime": "2026-01-30T00:00:00+06:00"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.sum").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("Should return 400 when sum is zero or negative")
    void shouldReturnBadRequestWhenSumNegativeOrZero() throws Exception {
        String json = """
                {
                  "account_from": "0000000123",
                  "account_to": "9999999999",
                  "currency_shortname": "KZT",
                  "sum": 0.00,
                  "expense_category": "PRODUCT",
                  "datetime": "2026-01-30T00:00:00+06:00"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.sum").value("sum must be strictly positive"));

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("Should return 400 when datetime is missing")
    void shouldReturnBadRequestWhenDatetimeMissing() throws Exception {
        String json = """
                {
                  "account_from": "0000000123",
                  "account_to": "9999999999",
                  "currency_shortname": "KZT",
                  "sum": 500.00,
                  "expense_category": "PRODUCT"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.datetime").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("Should return 400 when expense_category has invalid enum value")
    void shouldReturnBadRequestWhenCategoryEnumInvalid() throws Exception {
        String json = """
                {
                  "account_from": "0000000123",
                  "account_to": "9999999999",
                  "currency_shortname": "KZT",
                  "sum": 500.00,
                  "expense_category": "UNKNOWN_CATEGORY",
                  "datetime": "2026-01-30T00:00:00+06:00"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed JSON"));

        verifyNoInteractions(transactionService);
    }
}