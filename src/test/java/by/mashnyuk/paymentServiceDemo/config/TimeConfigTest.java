package by.mashnyuk.paymentServiceDemo.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import static org.assertj.core.api.Assertions.assertThat;

class TimeConfigTest {

    @Test
    void shouldCreateClockUsingApplicationZone() {
        TimeConfig config = new TimeConfig();

        ZoneId zoneId = ZoneId.of("Europe/Minsk");

        Clock clock = config.clock(zoneId);

        assertThat(clock.getZone())
                .isEqualTo(zoneId);
    }
}