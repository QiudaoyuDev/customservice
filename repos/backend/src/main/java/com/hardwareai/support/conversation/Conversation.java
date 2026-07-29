package com.hardwareai.support.conversation;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Anonymous support session anchored only to a server-resolved QR product scope.
 */
@Entity
@Table(name = "conversations")
class Conversation {
    @Id
    private UUID id;
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "qr_binding_id")
    private UUID qrBindingId;
    private String language, region;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
    @Column(name = "closed_at")
    private Instant closedAt;
    @Column(name = "current_flow_id")
    private UUID currentFlowId;
    @Column(name = "current_node_key")
    private String currentNodeKey;
    @Column(name = "flow_failures")
    private int flowFailures;
    @Column(name = "public_access_token_hash")
    private String publicAccessTokenHash;

    protected Conversation() {
    }

    Conversation(UUID tenant, UUID qr, String language, String region, String publicAccessTokenHash) {
        id = UUID.randomUUID();
        tenantId = tenant;
        qrBindingId = qr;
        this.language = language;
        this.region = region;
        this.publicAccessTokenHash = publicAccessTokenHash;
        status = Status.OPEN;
    }

    UUID id() {
        return id;
    }

    UUID tenantId() {
        return tenantId;
    }

    String language() {
        return language;
    }

    String region() {
        return region;
    }

    Status status() {
        return status;
    }

    UUID currentFlowId() {
        return currentFlowId;
    }

    String currentNodeKey() {
        return currentNodeKey;
    }

    int flowFailures() {
        return flowFailures;
    }

    boolean authorizes(String tokenHash) {
        return publicAccessTokenHash != null && publicAccessTokenHash.equals(tokenHash);
    }

    void startFlow(UUID flowId) {
        currentFlowId = flowId;
        currentNodeKey = null;
        flowFailures = 0;
    }

    void setNode(String key, int failures) {
        currentNodeKey = key;
        flowFailures = failures;
    }

    void clearFlow() {
        currentFlowId = null;
        currentNodeKey = null;
        flowFailures = 0;
    }

    enum Status {OPEN, CLOSED}
}
