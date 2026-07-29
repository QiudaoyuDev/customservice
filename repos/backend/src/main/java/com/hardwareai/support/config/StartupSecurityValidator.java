package com.hardwareai.support.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Refuses unsafe bootstrap values outside the explicitly local development profile.
 */
@Component
class StartupSecurityValidator implements ApplicationRunner {
    private final AppProperties properties;
    private final Environment environment;

    StartupSecurityValidator(AppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean development = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        List<String> secrets = List.of(properties.security().jwtSecret(), properties.qr().secret(),
                properties.storage().secretKey(), properties.qdrantApiKey(), properties.bootstrap().adminPassword(),
                properties.modelKeyEncryption().masterKey());
        if (!development && secrets.stream().anyMatch(this::isDefaultOrWeak)) {
            throw new IllegalStateException("Refusing startup with default or weak secret outside the dev profile");
        }
        if (!development && (properties.security().jwtSecret().length() < 32 || properties.qr().secret().length() < 32)) {
            throw new IllegalStateException("JWT_SECRET and QR_TOKEN_SECRET must each contain at least 32 characters");
        }
        try {
            if (java.util.Base64.getDecoder().decode(properties.modelKeyEncryption().masterKey()).length != 32)
                throw new IllegalStateException("MODEL_KEY_ENCRYPTION_MASTER_KEY must decode to exactly 32 bytes");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MODEL_KEY_ENCRYPTION_MASTER_KEY must be base64", exception);
        }
    }

    private boolean isDefaultOrWeak(String value) {
        return value == null || value.startsWith("CHANGE_ME") || value.length() < 16;
    }
}
