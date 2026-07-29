package com.hardwareai.support.handoff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Auditable internal note; customer-visible summaries are never overwritten.
 */
@Entity
@Table(name = "handoff_notes")
class HandoffNote {
    @Id
    private UUID id;
    @Column(name = "handoff_id")
    private UUID handoffId;
    @Column(name = "author_id")
    private UUID authorId;
    @Column(columnDefinition = "text")
    private String content;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected HandoffNote() {
    }

    HandoffNote(UUID handoffId, UUID authorId, String content) {
        id = UUID.randomUUID();
        this.handoffId = handoffId;
        this.authorId = authorId;
        this.content = content;
    }

    UUID id() {
        return id;
    }

    UUID authorId() {
        return authorId;
    }

    String content() {
        return content;
    }

    Instant createdAt() {
        return createdAt;
    }
}
