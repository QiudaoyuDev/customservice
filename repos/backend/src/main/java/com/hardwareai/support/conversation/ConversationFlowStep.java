package com.hardwareai.support.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Immutable audit record for a normalized reply applied to a flow node. */
@Entity
@Table(name = "conversation_flow_steps")
class ConversationFlowStep {
    @Id private UUID id;
    @Column(name = "flow_session_id") private UUID flowSessionId;
    @Column(name = "node_key") private String nodeKey;
    @Column(name = "normalized_reply") private String normalizedReply;
    @Column(name = "raw_message_id") private UUID rawMessageId;
    private String result;
    @Column(name = "created_at") private Instant createdAt = Instant.now();

    protected ConversationFlowStep() { }

    ConversationFlowStep(UUID flowSessionId, String nodeKey, String normalizedReply, UUID rawMessageId, String result) {
        id = UUID.randomUUID();
        this.flowSessionId = flowSessionId;
        this.nodeKey = nodeKey;
        this.normalizedReply = normalizedReply;
        this.rawMessageId = rawMessageId;
        this.result = result;
    }
    UUID flowSessionId() { return flowSessionId; }
    String nodeKey() { return nodeKey; }
    String normalizedReply() { return normalizedReply; }
    UUID rawMessageId() { return rawMessageId; }
    String result() { return result; }
    Instant createdAt() { return createdAt; }
}
