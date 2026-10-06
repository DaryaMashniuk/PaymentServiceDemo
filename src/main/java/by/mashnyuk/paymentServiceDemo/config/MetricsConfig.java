package by.mashnyuk.paymentServiceDemo.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public MetricsConfig(MeterRegistry meterRegistry) {
        meterRegistry.config()
                .commonTags("application", "payment-service-demo");
    }
}