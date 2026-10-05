package by.mashnyuk.paymentServiceDemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    public ZoneId applicationZoneId(
            @Value("${app.zone-id}") String zoneId
    ) {
        return ZoneId.of(zoneId);
    }

    @Bean
    public Clock clock(ZoneId applicationZoneId) {
        return Clock.system(applicationZoneId);
    }
}
