package com.hardwareai.support.knowledge;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable work item; a restart cannot silently lose parse or index work.
 */
@Entity
@Table(name = "index_jobs")
class ProcessingJob {

    @Id
    private UUID id;

    @Column(name = "revision_id")
    private UUID revisionId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "job_type")
    @Enumerated(EnumType.STRING)
    private Type jobType;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ProcessingJob() {
    }

    ProcessingJob(UUID revisionId, Type type) {
        id = UUID.randomUUID();
        this.revisionId = revisionId;
        jobType = type;
        status = Status.PENDING;
    }

    UUID id() {
        return id;
    }

    UUID revisionId() {
        return revisionId;
    }

    Type jobType() {
        return jobType;
    }

    void start() {
        status = Status.RUNNING;
        attempts++;
        startedAt = Instant.now();
    }

    void complete() {
        status = Status.COMPLETED;
        completedAt = Instant.now();
        errorMessage = null;
    }

    void fail(Exception e) {
        status = attempts >= 3 ? Status.FAILED : Status.PENDING;
        errorMessage = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    enum Type {
        PARSE,
        INDEX,
    }

    enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
    }
}
