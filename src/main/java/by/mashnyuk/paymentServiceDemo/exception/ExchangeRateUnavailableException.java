package by.mashnyuk.paymentServiceDemo.exception;

import by.mashnyuk.paymentServiceDemo.model.ExchangeRate;

public class ExchangeRateUnavailableException extends RuntimeException {
    public ExchangeRateUnavailableException(String message) {
        super(message);
    }

    public  ExchangeRateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExchangeRateUnavailableException(Throwable cause) {
        super(cause);
    }

    public  ExchangeRateUnavailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
