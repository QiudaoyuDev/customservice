package com.hardwareai.support.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Links a visible answer only to evidence selected during that exact answer run.
 */
@Entity
@Table(name = "answer_citations")
class AnswerCitation {
    @Id
    private UUID id;
    @Column(name = "answer_trace_id")
    private UUID answerTraceId;
    @Column(name = "revision_id")
    private UUID revisionId;
    @Column(name = "chunk_id")
    private UUID chunkId;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected AnswerCitation() {
    }

    AnswerCitation(UUID traceId, UUID revisionId, UUID chunkId) {
        id = UUID.randomUUID();
        answerTraceId = traceId;
        this.revisionId = revisionId;
        this.chunkId = chunkId;
    }

    UUID answerTraceId() {
        return answerTraceId;
    }

    UUID revisionId() {
        return revisionId;
    }

    UUID chunkId() {
        return chunkId;
    }
}
