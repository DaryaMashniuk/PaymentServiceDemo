package by.mashnyuk.paymentServiceDemo.service;


import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Limit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface LimitService {
    Limit setLimit(String accountFrom, BigDecimal limitSum, ExpenseCategory category);
    Limit getActualLimit(String accountFrom, ExpenseCategory category, OffsetDateTime datetime);
    List<Limit> getAllLimits(String accountFrom);
}
