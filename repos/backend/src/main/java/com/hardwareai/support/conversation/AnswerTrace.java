package com.hardwareai.support.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit record for a generated or safely degraded answer.
 */
@Entity
@Table(name = "answer_traces")
class AnswerTrace {
    @Id
    private UUID id;
    @Column(name = "conversation_id")
    private UUID conversationId;
    @Column(name = "message_id")
    private UUID messageId;
    @Column(name = "model_configuration_id")
    private UUID modelConfigurationId;
    private String intent;
    private String outcome;
    @Column(name = "latency_ms")
    private Long latencyMs;
    @Column(name = "token_count")
    private Integer tokenCount;
    @Column(name = "prompt_version")
    private String promptVersion;
    @Column(name = "finish_reason")
    private String finishReason;
    @Column(name = "retrieval_count")
    private int retrievalCount;
    @Column(name = "selected_evidence_count")
    private int selectedEvidenceCount;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected AnswerTrace() {
    }

    AnswerTrace(UUID conversationId, UUID messageId, UUID modelConfigurationId, String intent, String outcome, long latencyMs,
        String finishReason, int retrievalCount, int selectedEvidenceCount) {
        id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.intent = intent;
        this.outcome = outcome;
        this.modelConfigurationId = modelConfigurationId;
        this.latencyMs = latencyMs;
        this.finishReason = finishReason;
        this.promptVersion = "grounded-v1";
        this.retrievalCount = retrievalCount;
        this.selectedEvidenceCount = selectedEvidenceCount;
    }

    UUID id() {
        return id;
    }

    UUID messageId() {
        return messageId;
    }

    String intent() {
        return intent;
    }

    String outcome() {
        return outcome;
    }

    Long latencyMs() {
        return latencyMs;
    }

    String promptVersion() {
        return promptVersion;
    }

    String finishReason() {
        return finishReason;
    }

    int retrievalCount() {
        return retrievalCount;
    }

    int selectedEvidenceCount() {
        return selectedEvidenceCount;
    }

    Instant createdAt() {
        return createdAt;
    }
}
