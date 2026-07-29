package com.hardwareai.support.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface EvaluationCaseRepository extends JpaRepository<EvaluationCase, UUID> {
    List<EvaluationCase> findAllByTenantIdOrderByCreatedAtDesc(UUID tenant);
}
