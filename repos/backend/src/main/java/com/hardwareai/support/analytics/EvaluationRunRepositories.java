package com.hardwareai.support.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface EvaluationRunRepository extends JpaRepository<EvaluationRun, UUID> {
    Optional<EvaluationRun> findByIdAndTenantId(UUID id, UUID tenant);

    List<EvaluationRun> findAllByTenantIdOrderByCreatedAtDesc(UUID tenant);
}

interface EvaluationResultRepository extends JpaRepository<EvaluationResult, UUID> {
    List<EvaluationResult> findAllByEvaluationRunId(UUID runId);
}
