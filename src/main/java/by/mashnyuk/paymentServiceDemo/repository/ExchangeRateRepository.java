package by.mashnyuk.paymentServiceDemo.repository;

import by.mashnyuk.paymentServiceDemo.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository
        extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyPairAndRateDate(
            String currencyPair,
            LocalDate rateDate
    );

    @Query("""
            SELECT er
            FROM ExchangeRate er
            WHERE er.currencyPair = :currencyPair
              AND er.rateDate <= :date
            ORDER BY er.rateDate DESC
            LIMIT 1
            """)
    Optional<ExchangeRate> findLatestRateBefore(
            @Param("currencyPair") String currencyPair,
            @Param("date") LocalDate date);}