package by.mashnyuk.paymentServiceDemo.service;

import by.mashnyuk.paymentServiceDemo.model.Transaction;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExceededTransactionResponseDto;

import java.util.List;

public interface TransactionService {
    Transaction processTransaction(Transaction transaction);
    List<ExceededTransactionResponseDto> getExceededTransactions(String accountFrom);
}