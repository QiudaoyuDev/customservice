package com.hardwareai.support.product;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Product applicability anchor used to prevent cross-model knowledge retrieval.
 */
@Entity
@Table(name = "product_models")
public class ProductModel {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    private String family, model;

    @Column(name = "display_name")
    private String displayName;

    private String region;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected ProductModel() {
    }

    ProductModel(UUID tenantId, String family, String model, String displayName, String region) {
        id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.family = family;
        this.model = model;
        this.displayName = displayName;
        this.region = region;
        status = Status.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public String family() {
        return family;
    }

    public String model() {
        return model;
    }

    public String displayName() {
        return displayName;
    }

    public String region() {
        return region;
    }

    public Status status() {
        return status;
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
    }
}
