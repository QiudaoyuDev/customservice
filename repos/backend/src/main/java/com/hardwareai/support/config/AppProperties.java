package com.hardwareai.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "app")
@Validated
/** External configuration; credentials are supplied only through environment variables. */
public record AppProperties(
    @Valid Security security,
    @Valid Storage storage,
    @Valid Qr qr,
    @Valid Bootstrap bootstrap,
    @NotBlank String embeddingUrl,
    @NotBlank String ocrUrl,
    @NotBlank String qdrantUrl,
    @NotBlank String qdrantApiKey,
    String qdrantCollection
) {
    public record Security (@NotBlank String jwtSecret){
    }

    public record Storage (@NotBlank String endpoint, @NotBlank String accessKey, @NotBlank String secretKey, @NotBlank String bucket){
    }

    public record Qr (@NotBlank String secret){
    }

    public record Bootstrap (@NotBlank String adminEmail, @NotBlank String adminPassword, @NotBlank String tenantName){
    }
}
