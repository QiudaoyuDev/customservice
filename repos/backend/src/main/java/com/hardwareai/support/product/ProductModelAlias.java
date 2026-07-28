package com.hardwareai.support.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-scoped alternate model name used only for product identification.
 */
@Entity
@Table(name = "product_model_aliases")
class ProductModelAlias {
    @Id
    private UUID id;
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "product_model_id")
    private UUID productModelId;
    private String alias;
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected ProductModelAlias() {
    }

    ProductModelAlias(UUID tenantId, UUID productModelId, String alias) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.productModelId = productModelId;
        this.alias = alias;
    }

    String alias() {
        return alias;
    }
}
