package com.hardwareai.support.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareai.support.retrieval.RetrievalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Executes evaluation cases through the production retrieval interface, never a test-only shortcut.
 */
@Service
class EvaluationService {
    private final EvaluationCaseRepository cases;
    private final EvaluationRunRepository runs;
    private final EvaluationResultRepository results;
    private final RetrievalService retrieval;
    private final ObjectMapper json;

    EvaluationService(EvaluationCaseRepository cases, EvaluationRunRepository runs, EvaluationResultRepository results,
        RetrievalService retrieval, ObjectMapper json) {
        this.cases = cases;
        this.runs = runs;
        this.results = results;
        this.retrieval = retrieval;
        this.json = json;
    }

    @Transactional
    EvaluationRun run(UUID tenant, String label, String knowledge, String model, String retrievalVersion) {
        var run = runs.save(new EvaluationRun(tenant, label, knowledge, model, retrievalVersion));
        for (var item : cases.findAllByTenantIdOrderByCreatedAtDesc(tenant)) {
            if (!item.active()) continue;
            var result = retrieval.retrieve(
                new RetrievalService.RetrievalRequest(tenant, item.productModelId(), item.productVariantId(), item.region(),
                    item.hardwareRevision(), item.firmwareVersion(), item.language(), item.question(), null, 10, 0));
            String outcome = result.conflictDetected() ? "CONFLICT" : result.evidence().isEmpty() ? "NO_EVIDENCE" : "EVIDENCE_FOUND";
            int count = result.evidence().size();
            double score = (item.expectedOutcome().equals(outcome) ? 0.7 : 0) + (count >= item.expectedCitations() ? 0.3 : 0);
            try {
                results.save(new EvaluationResult(run.id(), item.id(), outcome, score,
                    json.writeValueAsString(Map.of("citationCount", count, "conflict", result.conflictDetected()))));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to persist evaluation result", e);
            }
        }
        return run;
    }
}
