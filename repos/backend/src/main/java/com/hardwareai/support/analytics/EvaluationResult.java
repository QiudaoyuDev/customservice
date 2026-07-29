package com.hardwareai.support.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One deterministic retrieval evaluation result.
 */
@Entity
@Table(name = "evaluation_results")
class EvaluationResult {
    @Id
    private UUID id;
    @Column(name = "evaluation_run_id")
    private UUID evaluationRunId;
    @Column(name = "evaluation_case_id")
    private UUID evaluationCaseId;
    private String outcome;
    private Double score;
    @Column(columnDefinition = "jsonb")
    private String details;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected EvaluationResult() {
    }

    EvaluationResult(UUID run, UUID item, String outcome, double score, String details) {
        id = UUID.randomUUID();
        evaluationRunId = run;
        evaluationCaseId = item;
        this.outcome = outcome;
        this.score = score;
        this.details = details;
    }

    UUID id() {
        return id;
    }

    UUID evaluationCaseId() {
        return evaluationCaseId;
    }

    String outcome() {
        return outcome;
    }

    Double score() {
        return score;
    }

    String details() {
        return details;
    }
}
