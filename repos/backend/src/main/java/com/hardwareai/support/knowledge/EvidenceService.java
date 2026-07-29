package com.hardwareai.support.knowledge;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Server-side evidence selector; never accepts tenant scope from a browser request.
 */
@Service
public class EvidenceService {
    private final KnowledgeChunkRepository chunks;

    public EvidenceService(KnowledgeChunkRepository chunks) {
        this.chunks = chunks;
    }

    public List<Evidence> find(UUID tenantId, UUID productModelId, UUID productVariantId, String region,
        String hardwareRevision, String firmwareVersion, String language, String question, String errorCode, int topK) {
        return chunks.keywordSearch(tenantId, productModelId, productVariantId, region, hardwareRevision, firmwareVersion,
                language, question, errorCode, PageRequest.of(0, Math.max(1, Math.min(topK, 20))))
            .stream().map(c -> new Evidence(c.id(), c.sourceLabel(), c.content())).toList();
    }

    public record Evidence(UUID chunkId, String source, String text) {
    }
}
