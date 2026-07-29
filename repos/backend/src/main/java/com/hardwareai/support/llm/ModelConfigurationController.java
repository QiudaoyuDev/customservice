package com.hardwareai.support.llm;

import com.hardwareai.support.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Management API deliberately exposes configured=true instead of any provider secret.
 */
@RestController
@RequestMapping("/api/model-configurations")
@PreAuthorize("hasRole('ADMIN')")
class ModelConfigurationController {
    private final ModelConfigurationRepository configurations;
    private final ModelConfigurationService service;
    private final CurrentUser current;

    ModelConfigurationController(ModelConfigurationRepository configurations, ModelConfigurationService service,
        CurrentUser current) {
        this.configurations = configurations;
        this.service = service;
        this.current = current;
    }

    @GetMapping
    List<View> list() {
        return configurations.findAllByTenantIdOrderByName(current.tenantId()).stream().map(View::of).toList();
    }

    @PostMapping
    View create(@Valid @RequestBody Create request) {
        return View.of(service.create(current.tenantId(),
            new ModelConfigurationService.CreateCommand(request.name(), request.providerType(), request.baseUrl(),
                request.modelName(), request.visionModel(), request.apiKey(), request.timeoutMs(), request.temperature(),
                request.maxTokens(), request.enabled(), request.defaultConfig())));
    }

    @PostMapping("/{id}/test")
    TestResult test(@PathVariable UUID id) {
        return new TestResult(service.testConnection(current.tenantId(), id));
    }

    record Create(@NotBlank @Size(max = 120) String name, @NotBlank String providerType, @NotBlank String baseUrl,
                  @NotBlank String modelName,
                  String visionModel, @NotBlank @Size(max = 2000) String apiKey, @Min(1000) @Max(120000) int timeoutMs,
                  @Min(0) @Max(2) double temperature, @Min(1) @Max(16000) int maxTokens, boolean enabled, boolean defaultConfig) {
    }

    record View(UUID id, String name, String providerType, String baseUrl, String modelName, String visionModel, int timeoutMs,
                double temperature, int maxTokens, boolean enabled, boolean defaultConfig, boolean configured) {
        static View of(ModelConfiguration item) {
            return new View(item.id(), item.name(), item.providerType(), item.baseUrl(), item.modelName(), item.visionModel(),
                item.timeoutMs(), item.temperature(), item.maxTokens(), item.enabled(), item.defaultConfig(), true);
        }
    }

    record TestResult(boolean reachable) {
    }
}
