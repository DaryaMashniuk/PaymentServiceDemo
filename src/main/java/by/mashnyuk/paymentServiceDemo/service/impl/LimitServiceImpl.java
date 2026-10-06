package by.mashnyuk.paymentServiceDemo.service.impl;

import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Limit;
import by.mashnyuk.paymentServiceDemo.repository.LimitRepository;
import by.mashnyuk.paymentServiceDemo.service.LimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitServiceImpl implements LimitService {

    public static final BigDecimal DEFAULT_LIMIT_USD = new BigDecimal("1000.00");
    public static final String USD_CURRENCY = "USD";

    private final LimitRepository limitRepository;
    private final Clock clock;

    @Override
    @Transactional
    public Limit setLimit(String accountFrom, BigDecimal limitSum, ExpenseCategory category) {
        if (accountFrom == null || accountFrom.isBlank()) {
            throw new IllegalArgumentException("Account must not be blank");
        }
        if (limitSum == null || limitSum.signum() < 0) {
            throw new IllegalArgumentException("Limit must be greater than or equal to zero");
        }
        if (category == null) {
            throw new IllegalArgumentException("Expense category must not be null");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        Limit limit = Limit.builder()
                .accountFrom(accountFrom)
                .limitSum(limitSum.setScale(2, RoundingMode.HALF_UP))
                .limitDatetime(now)
                .limitCurrencyShortname(USD_CURRENCY)
                .expenseCategory(category)
                .build();

        return limitRepository.save(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Limit getActualLimit(String accountFrom, ExpenseCategory category, OffsetDateTime datetime) {
        if (accountFrom == null || accountFrom.isBlank()) {
            throw new IllegalArgumentException("Account must not be blank");
        }
        if (category == null) {
            throw new IllegalArgumentException("Expense category must not be null");
        }
        if (datetime == null) {
            throw new IllegalArgumentException("Datetime must not be null");
        }

        return limitRepository.findLatestValidLimit(accountFrom, category.name(), datetime)
                .orElseGet(() -> Limit.builder()
                        .accountFrom(accountFrom)
                        .limitSum(DEFAULT_LIMIT_USD)
                        .limitDatetime(datetime)
                        .limitCurrencyShortname(USD_CURRENCY)
                        .expenseCategory(category)
                        .build()
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Limit> getAllLimits(String accountFrom) {
        if (accountFrom == null || accountFrom.isBlank()) {
            throw new IllegalArgumentException("Account must not be blank");
        }

        return limitRepository.findAllByAccountFromOrderByLimitDatetimeDesc(accountFrom);
    }
}