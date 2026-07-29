package com.hardwareai.support.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Persisted execution cursor for one immutable diagnostic-flow snapshot. */
@Entity
@Table(name = "conversation_flow_sessions")
class ConversationFlowSession {
    @Id private UUID id;
    @Column(name = "conversation_id") private UUID conversationId;
    @Column(name = "flow_version_id") private UUID flowVersionId;
    @Column(name = "current_node_key") private String currentNodeKey;
    @Column(name = "failure_count") private int failureCount;
    private String status;
    @Column(name = "started_at") private Instant startedAt = Instant.now();
    @Column(name = "ended_at") private Instant endedAt;

    protected ConversationFlowSession() { }

    ConversationFlowSession(UUID conversationId, UUID flowVersionId, String currentNodeKey) {
        id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.flowVersionId = flowVersionId;
        this.currentNodeKey = currentNodeKey;
        status = "ACTIVE";
    }

    UUID id() { return id; }
    UUID conversationId() { return conversationId; }
    UUID flowVersionId() { return flowVersionId; }
    String currentNodeKey() { return currentNodeKey; }
    int failureCount() { return failureCount; }
    String status() { return status; }
    Instant startedAt() { return startedAt; }
    Instant endedAt() { return endedAt; }

    void advance(String nextNodeKey, int failures) {
        currentNodeKey = nextNodeKey;
        failureCount = failures;
    }

    void complete() {
        status = "COMPLETED";
        endedAt = Instant.now();
    }
}
