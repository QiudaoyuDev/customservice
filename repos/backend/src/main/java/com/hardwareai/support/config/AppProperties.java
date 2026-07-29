package com.hardwareai.support.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
@Validated
/** External configuration; credentials are supplied only through environment variables. */
public record AppProperties(
    @Valid Security security,
    @Valid Storage storage,
    @Valid Qr qr,
    @Valid Bootstrap bootstrap,
    @Valid Llm llm,
    @Valid ModelKeyEncryption modelKeyEncryption,
    @Valid ExternalClients externalClients,
    @NotBlank String embeddingUrl,
    @NotBlank String ocrUrl,
    @NotBlank String rerankUrl,
    @NotBlank String qdrantUrl,
    @NotBlank String qdrantApiKey,
    String qdrantCollection
) {
    public record Security(@NotBlank String jwtSecret) {
    }

    public record Storage(@NotBlank String endpoint, @NotBlank String accessKey, @NotBlank String secretKey,
                          @NotBlank String bucket) {
    }

    public record Qr(@NotBlank String secret) {
    }

    public record Bootstrap(@NotBlank String adminEmail, @NotBlank String adminPassword, @NotBlank String tenantName) {
    }

    public record Llm(boolean enabled, String baseUrl, String apiKey, String model) {
    }

    /**
     * 32-byte base64 key is loaded only from environment/KMS integration, never from the database.
     */
    public record ModelKeyEncryption(@NotBlank String masterKey, @NotBlank String keyVersion) {
    }

    /**
     * Shared bounded timings for PostgreSQL-adjacent external HTTP and object-storage clients.
     */
    public record ExternalClients(
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull Duration requestTimeout
    ) {
        public Duration effectiveResponseTimeout() {
            return readTimeout.compareTo(requestTimeout) < 0 ? readTimeout : requestTimeout;
        }
    }
}
