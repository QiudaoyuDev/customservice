package com.hardwareai.support.llm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-scoped provider configuration. API key material is ciphertext-only at rest.
 */
@Entity
@Table(name = "model_configurations")
class ModelConfiguration {
    @Id
    private UUID id;
    @Column(name = "tenant_id")
    private UUID tenantId;
    private String name;
    @Column(name = "provider_type")
    private String providerType;
    @Column(name = "base_url")
    private String baseUrl;
    @Column(name = "model_name")
    private String modelName;
    @Column(name = "vision_model")
    private String visionModel;
    @Column(name = "api_key_ciphertext")
    private String apiKeyCiphertext;
    @Column(name = "api_key_nonce")
    private String apiKeyNonce;
    @Column(name = "api_key_key_version")
    private String apiKeyKeyVersion;
    @Column(name = "timeout_ms")
    private int timeoutMs;
    private double temperature;
    @Column(name = "max_tokens")
    private int maxTokens;
    private boolean enabled;
    @Column(name = "is_default")
    private boolean defaultConfig;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected ModelConfiguration() {
    }

    ModelConfiguration(UUID tenantId, String name, String providerType, String baseUrl, String modelName, String visionModel,
        int timeoutMs, double temperature, int maxTokens, boolean enabled, boolean defaultConfig, ApiKeyCipher.Encrypted key) {
        id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.providerType = providerType;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.visionModel = visionModel;
        this.timeoutMs = timeoutMs;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.enabled = enabled;
        this.defaultConfig = defaultConfig;
        apiKeyCiphertext = key.ciphertext();
        apiKeyNonce = key.nonce();
        apiKeyKeyVersion = key.keyVersion();
    }

    UUID id() {
        return id;
    }

    UUID tenantId() {
        return tenantId;
    }

    String name() {
        return name;
    }

    String providerType() {
        return providerType;
    }

    String baseUrl() {
        return baseUrl;
    }

    String modelName() {
        return modelName;
    }

    String visionModel() {
        return visionModel;
    }

    int timeoutMs() {
        return timeoutMs;
    }

    double temperature() {
        return temperature;
    }

    int maxTokens() {
        return maxTokens;
    }

    boolean enabled() {
        return enabled;
    }

    boolean defaultConfig() {
        return defaultConfig;
    }

    String decryptApiKey(ApiKeyCipher cipher) {
        return cipher.decrypt(apiKeyCiphertext, apiKeyNonce, apiKeyKeyVersion);
    }

    void clearDefault() {
        defaultConfig = false;
    }
}
