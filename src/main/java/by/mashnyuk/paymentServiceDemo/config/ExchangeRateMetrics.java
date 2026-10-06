package by.mashnyuk.paymentServiceDemo.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateMetrics {

    private final MeterRegistry meterRegistry;

    public ExchangeRateMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordSuccess(
            String pair,
            Timer.Sample sample
    ) {
        counter(
                "success",
                pair
        ).increment();

        sample.stop(
                latencyTimer(pair)
        );
    }

    public void recordError(
            String pair,
            Timer.Sample sample
    ) {
        counter(
                "error",
                pair
        ).increment();

        sample.stop(
                latencyTimer(pair)
        );
    }

    private Counter counter(
            String status,
            String pair
    ) {
        return Counter.builder(
                        "external_api_requests_total"
                )
                .description(
                        "Number of external exchange API requests"
                )
                .tag("status", status)
                .tag("pair", pair)
                .register(meterRegistry);
    }

    private Timer latencyTimer(String pair) {
        return Timer.builder(
                        "external_api_latency_seconds"
                )
                .description(
                        "Latency of external exchange API requests"
                )
                .tag("pair", pair)
                .publishPercentiles(
                        0.5,
                        0.95,
                        0.99
                )
                .register(meterRegistry);
    }
}
