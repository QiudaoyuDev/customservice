package com.hardwareai.support.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "max_attempts")
    private final int maxAttempts = 3;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "heartbeat_at")
    private Instant heartbeatAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

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

    boolean exhausted() {
        return attempts >= maxAttempts;
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
        errorCode = null;
        leaseUntil = null;
        heartbeatAt = Instant.now();
    }

    void fail(Exception e) {
        status = exhausted() ? Status.FAILED : Status.PENDING;
        errorMessage = e.getClass().getSimpleName();
        errorCode = e.getClass().getSimpleName();
        leaseUntil = null;
        heartbeatAt = Instant.now();
        // Bounded backoff avoids a broken source monopolising the worker.
        nextRetryAt = exhausted() ? null : Instant.now().plusSeconds(5L * attempts);
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
