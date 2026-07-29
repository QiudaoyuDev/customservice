package com.hardwareai.support.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the complete immutable Flyway migration chain against a real PostgreSQL instance when Docker is available. */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class FlywayPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.4-alpine")
            .withDatabaseName("support_test")
            .withUsername("support")
            .withPassword("support-password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.security.jwt-secret", () -> "test-jwt-secret-with-at-least-thirty-two-characters");
        registry.add("app.qr.secret", () -> "test-qr-secret-with-at-least-thirty-two-characters");
        registry.add("app.storage.secret-key", () -> "test-storage-secret-that-is-not-a-default");
        registry.add("app.qdrant-api-key", () -> "test-qdrant-key-that-is-not-a-default");
        registry.add("app.bootstrap.admin-password", () -> "test-admin-password-that-is-not-a-default");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migratesEveryVersionFromAnEmptyPostgresDatabase() {
        Integer migrations = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        assertThat(migrations).isGreaterThanOrEqualTo(9);
    }
}
