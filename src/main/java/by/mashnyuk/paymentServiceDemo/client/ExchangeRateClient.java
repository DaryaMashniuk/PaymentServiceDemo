package by.mashnyuk.paymentServiceDemo.client;

import by.mashnyuk.paymentServiceDemo.model.dto.response.ExchangeRateApiResponse;

import java.time.LocalDate;


public interface ExchangeRateClient {

    ExchangeRateApiResponse getRate(
            String pair,
            LocalDate date
    );
}