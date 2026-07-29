package com.hardwareai.support.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-owned retrieval regression case with explicit server-controlled product scope.
 */
@Entity
@Table(name = "evaluation_cases")
class EvaluationCase {
    @Id
    private UUID id;
    @Column(name = "tenant_id")
    private UUID tenantId;
    private String name;
    @Column(columnDefinition = "text")
    private String question;
    @Column(name = "expected_outcome")
    private String expectedOutcome;
    @Column(name = "expected_citations")
    private int expectedCitations;
    @Column(name = "model_scope")
    private String modelScope;
    @Column(name = "product_model_id")
    private UUID productModelId;
    @Column(name = "product_variant_id")
    private UUID productVariantId;
    @Column(name = "hardware_revision")
    private String hardwareRevision;
    @Column(name = "firmware_version")
    private String firmwareVersion;
    private String region, language;
    @Column(name = "risk_level")
    private String riskLevel;
    private final boolean active = true;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected EvaluationCase() {
    }

    EvaluationCase(UUID tenant, String name, String question, String outcome, int citations, String model, UUID productModel,
        UUID productVariant, String hardwareRevision, String firmwareVersion, String region, String language, String risk) {
        id = UUID.randomUUID();
        tenantId = tenant;
        this.name = name;
        this.question = question;
        expectedOutcome = outcome;
        expectedCitations = citations;
        modelScope = model;
        productModelId = productModel;
        productVariantId = productVariant;
        this.hardwareRevision = hardwareRevision;
        this.firmwareVersion = firmwareVersion;
        this.region = region;
        this.language = language;
        riskLevel = risk;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    String question() {
        return question;
    }

    String expectedOutcome() {
        return expectedOutcome;
    }

    int expectedCitations() {
        return expectedCitations;
    }

    String riskLevel() {
        return riskLevel;
    }

    boolean active() {
        return active;
    }

    UUID productModelId() {
        return productModelId;
    }

    UUID productVariantId() {
        return productVariantId;
    }

    String hardwareRevision() {
        return hardwareRevision;
    }

    String firmwareVersion() {
        return firmwareVersion;
    }

    String region() {
        return region;
    }

    String language() {
        return language;
    }
}
