package com.hardwareai.support.retrieval;

import java.util.List;
import java.util.UUID;

/**
 * Server-only retrieval contract. Browser callers never provide tenant scope directly.
 */
public interface RetrievalService {
    RetrievalResult retrieve(RetrievalRequest request);

    record RetrievalRequest(UUID tenantId, UUID productModelId, UUID productVariantId, String region,
                            String hardwareRevision, String firmwareVersion, String locale,
                            String userQuestion, String errorCode, int topK, double threshold) {
    }

    record RetrievalResult(List<Evidence> evidence, boolean conflictDetected) {
    }

    record Evidence(UUID chunkId, UUID revisionId, String documentTitle, Integer page,
                    String titlePath, double score, String applicability, String excerpt) {
    }
}
