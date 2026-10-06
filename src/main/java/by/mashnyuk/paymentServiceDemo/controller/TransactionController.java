package by.mashnyuk.paymentServiceDemo.controller;

import by.mashnyuk.paymentServiceDemo.mapper.TransactionMapper;
import by.mashnyuk.paymentServiceDemo.model.Transaction;
import by.mashnyuk.paymentServiceDemo.model.dto.request.TransactionRequestDto;
import by.mashnyuk.paymentServiceDemo.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "API A: Banking Transactions", description = "Endpoints for bank transaction ingestion")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Receive and process a new transaction")
    public void processTransaction(@Valid @RequestBody TransactionRequestDto requestDto) {
        log.info("REST: Received transaction for account {}", requestDto.getAccountFrom());
        Transaction entity = transactionMapper.toEntity(requestDto);
        transactionService.processTransaction(entity);
    }
}