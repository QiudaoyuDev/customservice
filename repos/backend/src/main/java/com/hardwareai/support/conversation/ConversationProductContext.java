package com.hardwareai.support.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Versioned product scope: changing a model retains historical message applicability.
 */
@Entity
@Table(name = "conversation_product_contexts")
class ConversationProductContext {
    @Id
    private UUID id;
    @Column(name = "conversation_id")
    private UUID conversationId;
    @Column(name = "product_model_id")
    private UUID productModelId;
    @Column(name = "product_variant_id")
    private UUID productVariantId;
    @Column(name = "hardware_version")
    private String hardwareVersion;
    @Column(name = "hardware_revision")
    private String hardwareRevision;
    @Column(name = "firmware_version")
    private String firmwareVersion;
    private String source;
    @Column(name = "confirmed_by_user")
    private boolean confirmedByUser;
    private boolean active = true;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();
    @Column(name = "closed_at")
    private Instant closedAt;

    protected ConversationProductContext() {
    }

    ConversationProductContext(UUID conversationId, UUID productModelId, UUID productVariantId, String hardwareRevision,
        String hardware, String firmware, String source, boolean confirmedByUser) {
        id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.productModelId = productModelId;
        this.productVariantId = productVariantId;
        this.hardwareRevision = hardwareRevision;
        hardwareVersion = hardware;
        firmwareVersion = firmware;
        this.source = source;
        this.confirmedByUser = confirmedByUser;
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

    String hardwareVersion() {
        return hardwareVersion;
    }

    void close() {
        active = false;
        closedAt = Instant.now();
    }
}
