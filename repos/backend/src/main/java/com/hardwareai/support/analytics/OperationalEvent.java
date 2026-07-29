package com.hardwareai.support.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Privacy-safe event envelope; sensitive content is deliberately absent.
 */
@Entity
@Table(name = "operational_events")
class OperationalEvent {
    @Id
    private UUID id;
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "conversation_id")
    private UUID conversationId;
    @Column(name = "event_type")
    private String eventType;
    @Column(columnDefinition = "jsonb")
    private String attributes = "{}";
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected OperationalEvent() {
    }

    OperationalEvent(UUID tenantId, UUID conversationId, String eventType, String attributes) {
        id = UUID.randomUUID(); this.tenantId = tenantId; this.conversationId = conversationId; this.eventType = eventType; this.attributes = attributes;
    }
    String eventType() { return eventType; }
    String attributes() { return attributes; }
    Instant createdAt() { return createdAt; }
}
