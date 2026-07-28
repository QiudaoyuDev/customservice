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
    @Column(name = "hardware_version")
    private String hardwareVersion;
    @Column(name = "firmware_version")
    private String firmwareVersion;
    private String source;
    private boolean active = true;
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected ConversationProductContext() {
    }

    ConversationProductContext(UUID conversationId, UUID productModelId, String hardware, String firmware, String source) {
        id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.productModelId = productModelId;
        hardwareVersion = hardware;
        firmwareVersion = firmware;
        this.source = source;
    }

    UUID productModelId() {
        return productModelId;
    }

    String hardwareVersion() {
        return hardwareVersion;
    }

    void close() {
        active = false;
    }
}
