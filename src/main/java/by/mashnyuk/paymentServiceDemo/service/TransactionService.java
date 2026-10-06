package by.mashnyuk.paymentServiceDemo.service;

import by.mashnyuk.paymentServiceDemo.model.Transaction;

public interface TransactionService {
    Transaction processTransaction(Transaction transaction);
}