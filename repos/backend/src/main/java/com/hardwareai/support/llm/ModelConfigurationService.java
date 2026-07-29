package com.hardwareai.support.llm;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ModelConfigurationService {
    private final ModelConfigurationRepository configurations;
    private final ApiKeyCipher cipher;
    private final ChatModelProvider provider;

    ModelConfigurationService(ModelConfigurationRepository configurations, ApiKeyCipher cipher, ChatModelProvider provider) {
        this.configurations = configurations;
        this.cipher = cipher;
        this.provider = provider;
    }

    @Transactional
    ModelConfiguration create(UUID tenantId, CreateCommand command) {
        if (command.defaultConfig())
            configurations.findAllByTenantIdOrderByName(tenantId).forEach(existing -> existing.clearDefault());
        return configurations.save(
            new ModelConfiguration(tenantId, command.name(), command.providerType(), command.baseUrl(), command.modelName(),
                command.visionModel(), command.timeoutMs(), command.temperature(), command.maxTokens(), command.enabled(),
                command.defaultConfig(), cipher.encrypt(command.apiKey())));
    }

    boolean testConnection(UUID tenantId, UUID id) {
        var configuration = configurations.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Model configuration not found"));
        return provider.testConnection(configuration.baseUrl(), configuration.decryptApiKey(cipher));
    }

    /**
     * Returns no value when a tenant deliberately has no enabled default model.
     */
    public Optional<GeneratedCompletion> generate(UUID tenantId, String system, String prompt) {
        return configurations.findByTenantIdAndDefaultConfigTrueAndEnabledTrue(tenantId).map(configuration ->
            new GeneratedCompletion(
                provider.complete(configuration.baseUrl(), configuration.decryptApiKey(cipher), configuration.modelName(),
                    system, prompt, configuration.temperature(), configuration.maxTokens()), configuration.id()));
    }

    public record GeneratedCompletion(String content, UUID configurationId) {
    }

    record CreateCommand(String name, String providerType, String baseUrl, String modelName, String visionModel, String apiKey,
                         int timeoutMs, double temperature, int maxTokens, boolean enabled, boolean defaultConfig) {
    }
}
