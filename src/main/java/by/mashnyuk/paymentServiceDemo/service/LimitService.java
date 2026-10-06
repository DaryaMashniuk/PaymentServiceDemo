package by.mashnyuk.paymentServiceDemo.service;

import by.mashnyuk.paymentServiceDemo.model.ExpenseCategory;
import by.mashnyuk.paymentServiceDemo.model.Limit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service for managing monthly spending limits for clients.
 * <p>
 * Ensures the immutability of historical limits: existing records are never updated;
 * each new limit is recorded with a timestamp provided by the system {@link java.time.Clock} bean.
 */
public interface LimitService {

    /**
     * Sets a new spending limit for a client.
     * <p>
     * The creation timestamp is automatically assigned by the server using current system time.
     *
     * @param accountFrom the client's 10-digit bank account number
     * @param limitSum    the limit amount in US Dollars (USD)
     * @param category    the expense category (goods or services)
     * @return the created {@link Limit} entity
     * @throws IllegalArgumentException if input parameters are invalid
     */
    Limit setLimit(String accountFrom, BigDecimal limitSum, ExpenseCategory category);

    /**
     * Retrieves the spending limit active for a client at a specific point in time.
     * <p>
     * If no custom limit was previously set for the specified category,
     * returns a virtual default limit of 1000.00 USD.
     *
     * @param accountFrom the client's bank account number
     * @param category    the expense category
     * @param datetime    the transaction timestamp
     * @return the active custom or default {@link Limit}
     */
    Limit getActualLimit(String accountFrom, ExpenseCategory category, OffsetDateTime datetime);

    /**
     * Retrieves the complete limit history for a client account, ordered descending by date.
     *
     * @param accountFrom the client's bank account number
     * @return a list of historical limit records
     */
    List<Limit> getAllLimits(String accountFrom);
}