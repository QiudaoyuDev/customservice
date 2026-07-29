package com.hardwareai.support.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable applicability fence for a revision; retrieval may only use an explicitly matching row.
 */
@Entity
@Table(name = "knowledge_revision_applicability")
class KnowledgeRevisionApplicability {
    @Id
    private UUID id;
    @Column(name = "revision_id")
    private UUID revisionId;
    @Column(name = "product_model_id")
    private UUID productModelId;
    @Column(name = "product_variant_id")
    private UUID productVariantId;
    private String region;
    @Column(name = "hardware_revision")
    private String hardwareRevision;
    @Column(name = "firmware_min")
    private String firmwareMin;
    @Column(name = "firmware_max")
    private String firmwareMax;
    @Column(name = "valid_from")
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;

    protected KnowledgeRevisionApplicability() {
    }

    KnowledgeRevisionApplicability(UUID revisionId, UUID productModelId, UUID productVariantId, String region,
        String hardwareRevision, String firmwareMin, String firmwareMax,
        Instant validFrom, Instant validTo) {
        this.id = UUID.randomUUID();
        this.revisionId = revisionId;
        this.productModelId = productModelId;
        this.productVariantId = productVariantId;
        this.region = region;
        this.hardwareRevision = hardwareRevision;
        this.firmwareMin = firmwareMin;
        this.firmwareMax = firmwareMax;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    UUID productVariantId() {
        return productVariantId;
    }

    UUID productModelId() {
        return productModelId;
    }

    String region() {
        return region;
    }

    String hardwareRevision() {
        return hardwareRevision;
    }

    String firmwareMin() {
        return firmwareMin;
    }

    String firmwareMax() {
        return firmwareMax;
    }

    Instant validFrom() {
        return validFrom;
    }

    Instant validTo() {
        return validTo;
    }
}
