package com.hardwareai.support.handoff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.NORMAL;
    @Column(name = "sla_due_at")
    private Instant slaDueAt;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();
    @Column(name = "closed_at")
    private Instant closedAt;

    protected HandoffRequest() {
    }

    public HandoffRequest(UUID tenant, UUID conversation, String key, String reason, String summary, boolean contact) {
        this(tenant, conversation, key, reason, summary, null, contact, summary);
    }

    public HandoffRequest(UUID tenant, UUID conversation, String key, String reason, String summary, String contact,
        boolean contactAuthorized, String packageSnapshot) {
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
        slaDueAt = Instant.now().plus(java.time.Duration.ofHours(48));
    }

    public UUID id() {
        return id;
    }

    public UUID conversationId() {
        return conversationId;
    }

    public Status status() {
        return status;
    }

    public String reason() {
        return reason;
    }

    public String summary() {
        return summary;
    }

    public String contact() {
        return contact;
    }

    public boolean contactAuthorized() {
        return contactAuthorized;
    }

    public UUID assignedTo() {
        return assignedTo;
    }

    public Resolution resolution() {
        return resolution;
    }

    public Priority priority() {
        return priority;
    }

    public Instant slaDueAt() {
        return slaDueAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public String packageSnapshot() {
        return packageSnapshot;
    }

    UUID tenantId() {
        return tenantId;
    }

    void claim(UUID user) {
        if (status != Status.NEW && status != Status.FAILED_DELIVERY)
            throw new IllegalStateException("Only new or failed-delivery requests can be claimed");
        status = Status.ASSIGNED;
        assignedTo = user;
    }

    void transition(Status next) {
        if (!allowed(status, next)) throw new IllegalStateException("Unsupported handoff state transition");
        status = next;
    }

    void reprioritize(Priority priority, Instant slaDueAt) {
        this.priority = priority;
        this.slaDueAt = slaDueAt;
    }

    void close(Resolution resolution) {
        if (status != Status.ASSIGNED && status != Status.IN_PROGRESS && status != Status.RESOLVED)
            throw new IllegalStateException("Request must be assigned before close");
        status = Status.CLOSED;
        this.resolution = resolution;
        closedAt = Instant.now();
    }

    private static boolean allowed(Status from, Status to) {
        return switch (from) {
            case NEW -> to == Status.ASSIGNED || to == Status.FAILED_DELIVERY;
            case ASSIGNED ->
                to == Status.IN_PROGRESS || to == Status.WAITING_USER || to == Status.WAITING_PARTS || to == Status.RESOLVED;
            case IN_PROGRESS -> to == Status.WAITING_USER || to == Status.WAITING_PARTS || to == Status.RESOLVED;
            case WAITING_USER, WAITING_PARTS -> to == Status.IN_PROGRESS || to == Status.RESOLVED;
            case RESOLVED -> to == Status.CLOSED;
            case FAILED_DELIVERY -> to == Status.ASSIGNED;
            case CLOSED -> false;
        };
    }

    public enum Status {NEW, ASSIGNED, IN_PROGRESS, WAITING_USER, WAITING_PARTS, RESOLVED, CLOSED, FAILED_DELIVERY}

    public enum Priority {LOW, NORMAL, HIGH, URGENT}

    public enum Resolution {RESOLVED, WAITING_PARTS, WARRANTY, ABANDONED, DUPLICATE, PRODUCT_DEFECT}
}
