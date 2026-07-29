package com.hardwareai.support.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StartupSecurityValidatorTest {
    @Test
    void refusesDefaultCredentialsInProduction() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var validator = new StartupSecurityValidator(defaults(), environment);
        assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void permitsLocalDefaultsOnlyInDevelopment() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        var validator = new StartupSecurityValidator(defaults(), environment);
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments(new String[0])));
    }

    private AppProperties defaults() {
        return new AppProperties(
                new AppProperties.Security("CHANGE_ME_USE_A_32_BYTE_SECRET_AT_MINIMUM"),
                new AppProperties.Storage("http://localhost:9000", "minio", "CHANGE_ME_MINIO_PASSWORD", "assets"),
                new AppProperties.Qr("CHANGE_ME_QR_SECRET"),
                new AppProperties.Bootstrap("admin@example.local", "CHANGE_ME_BEFORE_USE", "Demo"),
                new AppProperties.Llm(false, "", "", ""),
                new AppProperties.ExternalClients(Duration.ofSeconds(3), Duration.ofSeconds(15), Duration.ofSeconds(20)),
                "http://localhost:18082", "http://localhost:18081", "http://localhost:18083", "http://localhost:6333",
                "CHANGE_ME_QDRANT_API_KEY", "knowledge"
        );
    }
}
