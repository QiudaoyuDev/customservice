package com.hardwareai.support.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareai.support.retrieval.RetrievalService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EvaluationServiceTest {
    @Test
    void persistsScoredResultFromProductionRetrievalContract() {
        var cases = mock(EvaluationCaseRepository.class);
        var runs = mock(EvaluationRunRepository.class);
        var results = mock(EvaluationResultRepository.class);
        var retrieval = mock(RetrievalService.class);
        var service = new EvaluationService(cases, runs, results, retrieval, new ObjectMapper());
        var tenant = UUID.randomUUID();
        var item = new EvaluationCase(tenant, "Evidence case", "How do I reset it?", "EVIDENCE_FOUND", 1, null,
                UUID.randomUUID(), null, null, "1.2.0", "US", "en", "LOW");
        when(cases.findAllByTenantIdOrderByCreatedAtDesc(tenant)).thenReturn(List.of(item));
        when(runs.save(any(EvaluationRun.class))).thenAnswer(call -> call.getArgument(0));
        when(retrieval.retrieve(any())).thenReturn(new RetrievalService.RetrievalResult(
                List.of(new RetrievalService.Evidence(UUID.randomUUID(), UUID.randomUUID(), "Guide", 1, "Reset", 0.9, "MATCH", "Press reset")), false));

        service.run(tenant, "nightly", "knowledge-1", "model-1", "retrieval-1");

        var captured = org.mockito.ArgumentCaptor.forClass(EvaluationResult.class);
        verify(results).save(captured.capture());
        assertEquals("EVIDENCE_FOUND", captured.getValue().outcome());
        assertEquals(1.0d, captured.getValue().score());
    }
}
