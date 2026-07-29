package com.hardwareai.support.knowledge;

import com.hardwareai.support.common.CurrentUser;
import com.hardwareai.support.retrieval.RetrievalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin retrieval probe using exactly the same tenant and applicability gates as conversations.
 */
@RestController
@RequestMapping("/api/search")
class KnowledgeSearchController {
    private final CurrentUser current;
    private final RetrievalService retrieval;

    KnowledgeSearchController(CurrentUser current, RetrievalService retrieval) {
        this.current = current;
        this.retrieval = retrieval;
    }

    @PostMapping
    Map<String, Object> search(@Valid @RequestBody Query query) {
        var request = new RetrievalService.RetrievalRequest(current.tenantId(), query.productModelId(), query.productVariantId(),
            query.region(), query.hardwareRevision(), query.firmwareVersion(), query.locale(), query.query(), query.errorCode(),
            query.limit(), 0.0d);
        var result = retrieval.retrieve(request);
        List<Result> results = result.evidence().stream().map(evidence -> new Result(evidence.chunkId(), evidence.documentTitle(),
            evidence.excerpt(), evidence.revisionId(), evidence.page(), evidence.score(), evidence.titlePath())).toList();
        return Map.of("filters", Map.of("productModelId", query.productModelId(), "productVariantId",
                query.productVariantId() == null ? "" : query.productVariantId(),
                "region", query.region(), "hardwareRevision", query.hardwareRevision() == null ? "" : query.hardwareRevision(),
                "firmwareVersion", query.firmwareVersion() == null ? "" : query.firmwareVersion(), "locale", query.locale()),
            "conflictDetected", result.conflictDetected(), "results", results);
    }

    record Query(@NotBlank @Size(max = 1000) String query, @NotNull UUID productModelId, UUID productVariantId,
                 @NotBlank @Size(max = 16) String region, @Size(max = 80) String hardwareRevision,
                 @Size(max = 80) String firmwareVersion, @Size(max = 80) String errorCode, @NotBlank @Size(max = 16) String locale,
                 @Min(1) @Max(10) int limit) {
    }

    record Result(UUID chunkId, String source, String text, UUID revisionId, Integer page, double score, String titlePath) {
    }
}
