package by.mashnyuk.paymentServiceDemo.service;

import by.mashnyuk.paymentServiceDemo.model.Transaction;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExceededTransactionResponseDto;

import java.util.List;

/**
 * Service for processing expense bank transactions.
 * <p>
 * Responsible for converting transaction amounts into the base accounting currency (USD),
 * thread-safe aggregation of monthly spending, and evaluating the {@code limit_exceeded} flag.
 */
public interface TransactionService {

    /**
     * Processes and persists an incoming transaction.
     * <p>
     * Locks the monthly spending aggregate, converts the amount to USD, matches the active
     * limit for the transaction timestamp, and flags limit breaches accordingly.
     *
     * @param transaction the incoming transaction entity
     * @return the persisted transaction with updated {@code sumUsd} and {@code limitExceeded} fields
     */
    Transaction processTransaction(Transaction transaction);

    /**
     * Retrieves all client transactions that exceeded the monthly spending limit,
     * enriched with historical limit details.
     *
     * @param accountFrom the client's 10-digit bank account number
     * @return a list of DTOs containing transaction details and their corresponding active limit
     */
    List<ExceededTransactionResponseDto> getExceededTransactions(String accountFrom);
}