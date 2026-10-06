package by.mashnyuk.paymentServiceDemo.exception;

public class ExchangeRateClientException
        extends RuntimeException {

    private final boolean retryable;

    public ExchangeRateClientException(
            String message,
            boolean retryable
    ) {
        super(message);
        this.retryable = retryable;
    }

    public ExchangeRateClientException(String message) {
        this(message, false);
    }

    public ExchangeRateClientException(
            String message,
            Throwable cause,
            boolean retryable
    ) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
