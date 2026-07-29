package com.hardwareai.support.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-owned sellable hardware revision. It is the public product context boundary below a model.
 */
@Entity
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "product_model_id", nullable = false)
    private UUID productModelId;
    private String region;
    @Column(name = "hardware_revision")
    private String hardwareRevision;
    private String sku;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "valid_from")
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected ProductVariant() {
    }

    ProductVariant(UUID tenantId, UUID productModelId, String region, String hardwareRevision, String sku,
        Instant validFrom, Instant validTo) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.productModelId = productModelId;
        this.region = region;
        this.hardwareRevision = hardwareRevision;
        this.sku = sku;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = Status.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID productModelId() {
        return productModelId;
    }

    public String region() {
        return region;
    }

    public String hardwareRevision() {
        return hardwareRevision;
    }

    public String sku() {
        return sku;
    }

    public Status status() {
        return status;
    }

    public boolean activeAt(Instant instant) {
        return status == Status.ACTIVE && (validFrom == null || !validFrom.isAfter(instant))
            && (validTo == null || validTo.isAfter(instant));
    }

    public enum Status {ACTIVE, ARCHIVED}
}
