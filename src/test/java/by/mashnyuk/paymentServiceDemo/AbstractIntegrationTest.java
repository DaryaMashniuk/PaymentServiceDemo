package by.mashnyuk.paymentServiceDemo;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("test")
                    .withUsername("test")
                    .withPassword("test");

    @ServiceConnection(name = "redis")
    static GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
        TRUNCATE TABLE
            transactions,
            limits,
            monthly_spending,
            exchange_rates
        RESTART IDENTITY CASCADE
        """);
    }
}