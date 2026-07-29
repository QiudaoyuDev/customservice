package com.hardwareai.support.handoff;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable human-support work item; notification retries never create a second request.
 */
@Entity
@Table(name = "handoff_requests")
public class HandoffRequest {
    @Id
    private UUID id;
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "conversation_id")
    private UUID conversationId;
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String reason;
    @Column(columnDefinition = "text")
    private String summary;
    @Column(name = "contact_authorized")
    private boolean contactAuthorized;
    private String contact;
    @Column(name = "package_snapshot", columnDefinition = "text")
    private String packageSnapshot;
    @Column(name = "assigned_to")
    private UUID assignedTo;
    @Enumerated(EnumType.STRING)
    private Resolution resolution;
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
    @Column(name = "closed_at")
    private Instant closedAt;

    protected HandoffRequest() {
    }

    public HandoffRequest(UUID tenant, UUID conversation, String key, String reason, String summary, boolean contact) {
        this(tenant, conversation, key, reason, summary, null, contact, summary);
    }

    public HandoffRequest(UUID tenant, UUID conversation, String key, String reason, String summary, String contact, boolean contactAuthorized, String packageSnapshot) {
        id = UUID.randomUUID();
        tenantId = tenant;
        conversationId = conversation;
        idempotencyKey = key;
        this.reason = reason;
        this.summary = summary;
        this.contact = contact;
        this.contactAuthorized = contactAuthorized;
        this.packageSnapshot = packageSnapshot;
        status = Status.NEW;
    }

    public UUID id() {
        return id;
    }

    public UUID conversationId() { return conversationId; }
    public Status status() { return status; }
    public String reason() { return reason; }
    public String summary() { return summary; }
    public String contact() { return contact; }
    public boolean contactAuthorized() { return contactAuthorized; }
    public UUID assignedTo() { return assignedTo; }
    public Resolution resolution() { return resolution; }
    public Instant createdAt() { return createdAt; }
    public Instant closedAt() { return closedAt; }
    public String packageSnapshot() { return packageSnapshot; }

    UUID tenantId() {
        return tenantId;
    }

    void claim(UUID user) {
        if (status != Status.NEW) throw new IllegalStateException("Only new requests can be claimed");
        status = Status.IN_PROGRESS;
        assignedTo = user;
    }

    void close(Resolution resolution) {
        if (status != Status.IN_PROGRESS) throw new IllegalStateException("Request must be claimed before close");
        status = Status.CLOSED;
        this.resolution = resolution;
        closedAt = Instant.now();
    }

    public enum Status {NEW, IN_PROGRESS, CLOSED}

    public enum Resolution {RESOLVED, WAITING_PARTS, WARRANTY, ABANDONED, DUPLICATE, PRODUCT_DEFECT}
}
