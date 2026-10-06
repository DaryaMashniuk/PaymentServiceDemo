package by.mashnyuk.paymentServiceDemo.controller;

import by.mashnyuk.paymentServiceDemo.mapper.LimitMapper;
import by.mashnyuk.paymentServiceDemo.model.Limit;
import by.mashnyuk.paymentServiceDemo.model.dto.request.LimitRequestDto;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExceededTransactionResponseDto;
import by.mashnyuk.paymentServiceDemo.model.dto.response.LimitResponseDto;
import by.mashnyuk.paymentServiceDemo.service.LimitService;
import by.mashnyuk.paymentServiceDemo.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "API B: Client Operations", description = "Endpoints for client limit management and inquiries")
public class ClientController {

    private final LimitService limitService;
    private final TransactionService transactionService;
    private final LimitMapper limitMapper;

    @PostMapping("/limits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Establish a new monthly spending limit")
    public LimitResponseDto setLimit(@Valid @RequestBody LimitRequestDto requestDto) {
        log.info("REST: Set new limit for account {}", requestDto.accountFrom());
        Limit limit = limitService.setLimit(
                requestDto.accountFrom(),
                requestDto.limitSum(),
                requestDto.expenseCategory()
        );
        return limitMapper.toDto(limit);
    }

    @GetMapping("/limits")
    @Operation(summary = "Get all established limits for account")
    public List<LimitResponseDto> getAllLimits(
            @RequestParam("account")
            @Pattern(regexp = "^\\d{10}$", message = "account must be exactly 10 digits") String account) {
        log.info("REST: Query all limits for account {}", account);
        return limitService.getAllLimits(account).stream()
                .map(limitMapper::toDto)
                .toList();
    }

    @GetMapping("/transactions/exceeded")
    @Operation(summary = "Get list of transactions that exceeded the limit")
    public List<ExceededTransactionResponseDto> getExceededTransactions(
            @RequestParam("account")
            @Pattern(regexp = "^\\d{10}$", message = "account must be exactly 10 digits") String account) {
        log.info("REST: Query exceeded transactions for account {}", account);
        return transactionService.getExceededTransactions(account);
    }
}
