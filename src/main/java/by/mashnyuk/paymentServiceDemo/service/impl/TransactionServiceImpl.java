package by.mashnyuk.paymentServiceDemo.service.impl;

import by.mashnyuk.paymentServiceDemo.mapper.TransactionMapper;
import by.mashnyuk.paymentServiceDemo.model.Currency;
import by.mashnyuk.paymentServiceDemo.model.Limit;
import by.mashnyuk.paymentServiceDemo.model.Transaction;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExceededTransactionResponseDto;
import by.mashnyuk.paymentServiceDemo.repository.MonthlySpendingRepository;
import by.mashnyuk.paymentServiceDemo.repository.TransactionRepository;
import by.mashnyuk.paymentServiceDemo.service.ExchangeRateService;
import by.mashnyuk.paymentServiceDemo.service.LimitService;
import by.mashnyuk.paymentServiceDemo.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final MonthlySpendingRepository monthlySpendingRepository;
    private final ExchangeRateService exchangeRateService;
    private final LimitService limitService;
    private final ZoneId applicationZoneId;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ExceededTransactionResponseDto> getExceededTransactions(String accountFrom) {
        if (accountFrom == null || accountFrom.isBlank()) {
            throw new IllegalArgumentException("Account must not be blank");
        }
        List<Transaction> exceededTransactions =
                transactionRepository.findAllByAccountFromAndLimitExceededTrue(accountFrom);

        return exceededTransactions.stream()
                .map(tx -> {
                    Limit actualLimit = limitService.getActualLimit(
                            tx.getAccountFrom(),
                            tx.getExpenseCategory(),
                            tx.getDatetime()
                    );
                    return transactionMapper.toExceededDto(tx, actualLimit);
                })
                .toList();
    }

    @Override
    @Transactional
    public Transaction processTransaction(Transaction tx) {
        validateTransaction(tx);

        log.info("Processing transaction for account: {}, sum: {} {}",
                tx.getAccountFrom(), tx.getSum(), tx.getCurrencyShortname());

        BigDecimal sumUsd = calculateSumUsd(tx);
        tx.setSumUsd(sumUsd);

        LocalDate txDate = tx.getDatetime().atZoneSameInstant(applicationZoneId).toLocalDate();
        LocalDate monthStart = txDate.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);

        OffsetDateTime monthStartDateTime = monthStart.atStartOfDay(applicationZoneId).toOffsetDateTime();
        OffsetDateTime monthEndDateTime = nextMonthStart.atStartOfDay(applicationZoneId).toOffsetDateTime();

        Limit actualLimit = limitService.getActualLimit(
                tx.getAccountFrom(),
                tx.getExpenseCategory(),
                tx.getDatetime()
        );

        monthlySpendingRepository.createIfAbsent(
                tx.getAccountFrom(),
                tx.getExpenseCategory().name(),
                monthStart,
                actualLimit.getLimitSum()
        );

        monthlySpendingRepository.findForUpdateByAccountAndCategoryAndMonth(
                tx.getAccountFrom(),
                tx.getExpenseCategory(),
                monthStart
        ).orElseThrow(() -> new IllegalStateException("Monthly lock anchor could not be acquired"));

        List<Transaction> previousTransactions = transactionRepository.findTransactionsForUpdate(
                tx.getAccountFrom(),
                tx.getExpenseCategory().name(),
                monthStartDateTime,
                monthEndDateTime,
                tx.getDatetime()
        );

        BigDecimal previousMonthSpent = previousTransactions.stream()
                .map(Transaction::getSumUsd)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotalSpent = previousMonthSpent.add(sumUsd);

        boolean isExceeded = newTotalSpent.compareTo(actualLimit.getLimitSum()) > 0;
        tx.setLimitExceeded(isExceeded);

        log.debug("Tx account={}, prevSpent={}, txSum={}, total={}, limit={}, exceeded={}",
                tx.getAccountFrom(), previousMonthSpent, sumUsd, newTotalSpent, actualLimit.getLimitSum(), isExceeded);

        return transactionRepository.save(tx);
    }

    private BigDecimal calculateSumUsd(Transaction tx) {
        if (tx.getCurrencyShortname() == Currency.USD) {
            return tx.getSum().setScale(2, RoundingMode.HALF_UP);
        }

        LocalDate txDate = tx.getDatetime().atZoneSameInstant(applicationZoneId).toLocalDate();
        String pair = tx.getCurrencyShortname().name() + "/USD";

        BigDecimal rate = exchangeRateService.getRate(txDate, pair);
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalStateException("Exchange rate not available for pair: " + pair);
        }

        return tx.getSum()
                .multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateTransaction(Transaction tx) {
        if (tx == null) {
            throw new IllegalArgumentException("Transaction must not be null");
        }
        if (tx.getAccountFrom() == null || tx.getAccountFrom().isBlank()) {
            throw new IllegalArgumentException("Account must not be blank");
        }
        if (tx.getSum() == null || tx.getSum().signum() < 0) {
            throw new IllegalArgumentException("Transaction sum must be greater than or equal to zero");
        }
        if (tx.getCurrencyShortname() == null) {
            throw new IllegalArgumentException("Transaction currency must not be null");
        }
        if (tx.getDatetime() == null) {
            throw new IllegalArgumentException("Transaction datetime must not be null");
        }
        if (tx.getExpenseCategory() == null) {
            throw new IllegalArgumentException("Expense category must not be null");
        }
    }
}