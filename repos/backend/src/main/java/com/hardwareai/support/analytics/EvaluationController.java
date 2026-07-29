package com.hardwareai.support.analytics;

import com.hardwareai.support.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped regression dataset and execution endpoints.
 */
@RestController
@RequestMapping("/api/evaluations")
@PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
class EvaluationController {
    private final EvaluationCaseRepository cases;
    private final EvaluationRunRepository runs;
    private final EvaluationResultRepository results;
    private final EvaluationService evaluator;
    private final CurrentUser current;

    EvaluationController(EvaluationCaseRepository cases, EvaluationRunRepository runs, EvaluationResultRepository results,
        EvaluationService evaluator, CurrentUser current) {
        this.cases = cases;
        this.runs = runs;
        this.results = results;
        this.evaluator = evaluator;
        this.current = current;
    }

    @GetMapping
    List<View> list() {
        return cases.findAllByTenantIdOrderByCreatedAtDesc(current.tenantId()).stream().map(View::of).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    View create(@Valid @RequestBody Create request) {
        return View.of(cases.save(
            new EvaluationCase(current.tenantId(), request.name(), request.question(), request.expectedOutcome(),
                request.expectedCitations(), request.modelScope(), request.productModelId(), request.productVariantId(),
                request.hardwareRevision(), request.firmwareVersion(), request.region(), request.language(), request.riskLevel())));
    }

    @PostMapping("/runs")
    @PreAuthorize("hasRole('ADMIN')")
    RunView run(@Valid @RequestBody Run request) {
        return RunView.of(evaluator.run(current.tenantId(), request.label(), request.knowledgeVersion(), request.modelVersion(),
            request.retrievalVersion()));
    }

    @GetMapping("/runs")
    List<RunView> runs() {
        return runs.findAllByTenantIdOrderByCreatedAtDesc(current.tenantId()).stream().map(RunView::of).toList();
    }

    @GetMapping("/runs/{id}")
    RunDetail detail(@PathVariable UUID id) {
        var run = runs.findByIdAndTenantId(id, current.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Evaluation run not found"));
        return new RunDetail(RunView.of(run), results.findAllByEvaluationRunId(run.id()).stream().map(ResultView::of).toList());
    }

    record Create(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 4000) String question,
                  @NotBlank String expectedOutcome, @Min(0) @Max(10) int expectedCitations, String modelScope,
                  @NotNull UUID productModelId, UUID productVariantId, String hardwareRevision, String firmwareVersion,
                  @NotBlank String region, @NotBlank String language, String riskLevel) {
    }

    record Run(@NotBlank @Size(max = 200) String label, String knowledgeVersion, String modelVersion, String retrievalVersion) {
    }

    record View(UUID id, String name, String question, String expectedOutcome, int expectedCitations, String riskLevel,
                boolean active) {
        static View of(EvaluationCase value) {
            return new View(value.id(), value.name(), value.question(), value.expectedOutcome(), value.expectedCitations(),
                value.riskLevel(), value.active());
        }
    }

    record RunView(UUID id, String label, java.time.Instant createdAt) {
        static RunView of(EvaluationRun run) {
            return new RunView(run.id(), run.label(), run.createdAt());
        }
    }

    record ResultView(UUID id, UUID caseId, String outcome, Double score, String details) {
        static ResultView of(EvaluationResult value) {
            return new ResultView(value.id(), value.evaluationCaseId(), value.outcome(), value.score(), value.details());
        }
    }

    record RunDetail(RunView run, List<ResultView> results) {
    }
}
