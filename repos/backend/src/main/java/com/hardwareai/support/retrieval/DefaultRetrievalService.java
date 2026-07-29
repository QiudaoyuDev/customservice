package com.hardwareai.support.retrieval;

import com.hardwareai.support.config.AppProperties;
import com.hardwareai.support.config.ExternalRestClientFactory;
import com.hardwareai.support.knowledge.EmbeddingProvider;
import com.hardwareai.support.knowledge.EvidenceService;
import com.hardwareai.support.knowledge.RerankProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hybrid FTS/vector retrieval with deterministic FTS fallback when local model services are unavailable.
 */
@Service
class DefaultRetrievalService implements RetrievalService {
    private static final Logger log = LoggerFactory.getLogger(DefaultRetrievalService.class);
    private final EvidenceService lexical;
    private final RestClient qdrant;
    private final EmbeddingProvider embedding;
    private final AppProperties config;
    private final RerankProvider reranker;
    private final ConclusionConflictDetector conflictDetector = new ConclusionConflictDetector();

    DefaultRetrievalService(EvidenceService lexical, AppProperties config, ExternalRestClientFactory clients,
        RerankProvider reranker, EmbeddingProvider embedding) {
        this.lexical = lexical;
        this.config = config;
        this.qdrant = clients.create(config.qdrantUrl(), "api-key", config.qdrantApiKey());
        this.embedding = embedding;
        this.reranker = reranker;
    }

    @Override
    public RetrievalResult retrieve(RetrievalRequest request) {
        if (request.userQuestion() == null || request.userQuestion().isBlank()) return new RetrievalResult(List.of(), false);
        int limit = Math.max(1, Math.min(request.topK(), 10));
        var merged = new LinkedHashMap<UUID, Candidate>();
        var lexicalHits = lexical.find(request.tenantId(), request.productModelId(), request.productVariantId(), request.region(),
            request.hardwareRevision(), request.firmwareVersion(), request.locale(), request.userQuestion(), request.errorCode(),
            limit);
        for (int index = 0; index < lexicalHits.size(); index++) {
            var hit = lexicalHits.get(index);
            merged.put(hit.chunkId(),
                new Candidate(hit.chunkId(), null, hit.source(), null, hit.source(), reciprocalRank(index), request.region(),
                    hit.text()));
        }
        try {
            addVectorHits(merged, request, limit);
        } catch (Exception exception) {
            log.warn("Vector retrieval unavailable; using FTS only");
        }
        var ranked = new ArrayList<>(merged.values());
        ranked.sort((left, right) -> Double.compare(right.score, left.score));
        try {
            var order = reranker.rank(request.userQuestion(), ranked.stream().map(candidate -> candidate.excerpt).toList());
            var reranked = new ArrayList<Candidate>();
            for (Integer index : order) if (index != null && index >= 0 && index < ranked.size()) reranked.add(ranked.get(index));
            if (!reranked.isEmpty()) ranked = reranked;
        } catch (Exception exception) {
            log.warn("Local rerank unavailable; using RRF order");
        }
        var accepted = ranked.stream().filter(candidate -> candidate.score >= request.threshold()).limit(limit).toList();
        return new RetrievalResult(accepted.stream().map(Candidate::evidence).toList(),
            conflictDetector.conflicts(accepted.stream().map(candidate -> candidate.excerpt).toList()));
    }

    @SuppressWarnings("unchecked")
    private void addVectorHits(Map<UUID, Candidate> merged, RetrievalRequest request, int limit) {
        var vector = embedding.embed(request.userQuestion());
        var must = new ArrayList<Map<String, Object>>();
        must.add(Map.of("key", "tenantId", "match", Map.of("value", request.tenantId().toString())));
        must.add(Map.of("key", "productModelId", "match", Map.of("value", request.productModelId().toString())));
        must.add(Map.of("key", "region", "match", Map.of("value", request.region())));
        must.add(Map.of("key", "locale", "match", Map.of("value", request.locale())));
        must.add(Map.of("key", "status", "match", Map.of("value", "PUBLISHED")));
        if (request.productVariantId() != null) {
            must.add(Map.of("should", List.of(
                Map.of("key", "productVariantId", "match", Map.of("value", request.productVariantId().toString())),
                Map.of("is_empty", Map.of("key", "productVariantId"))
            )));
        }
        if (request.hardwareRevision() != null && !request.hardwareRevision().isBlank()) {
            must.add(Map.of("should", List.of(
                Map.of("key", "hardwareRevision", "match", Map.of("value", request.hardwareRevision())),
                Map.of("is_empty", Map.of("key", "hardwareRevision"))
            )));
        }
        // Qdrant cannot safely compare arbitrary semantic-version strings; version-scoped rows stay in PostgreSQL FTS.
        if (request.firmwareVersion() != null && !request.firmwareVersion().isBlank()) {
            must.add(Map.of("key", "firmwareScoped", "match", Map.of("value", false)));
        }
        var response = qdrant.post().uri("/collections/{collection}/points/query", config.qdrantCollection())
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("query", vector, "limit", limit, "with_payload", true,
                "filter", Map.of("must", must))).retrieve().body(Map.class);
        var result = response == null ? null : (Map<?, ?>)response.get("result");
        var points = result != null && result.get("points") instanceof List<?> values ? values : List.of();
        int rank = 0;
        for (Object raw : points) {
            if (!(raw instanceof Map<?, ?> point) || !(point.get("payload") instanceof Map<?, ?> payload)) continue;
            UUID id = UUID.fromString(String.valueOf(point.get("id")));
            String excerpt = String.valueOf(payload.get("text"));
            Candidate vectorHit = new Candidate(id, uuid(payload.get("revisionId")), String.valueOf(payload.get("source")),
                number(payload.get("page")), String.valueOf(payload.get("titlePath")), reciprocalRank(rank++), request.region(),
                excerpt);
            merged.merge(id, vectorHit, Candidate::merge);
        }
    }

    private static double reciprocalRank(int rank) {
        return 1.0d / (60 + rank + 1);
    }

    private static UUID uuid(Object value) {
        try {
            return value == null ? null : UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Integer number(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private record Candidate(UUID chunkId, UUID revisionId, String documentTitle, Integer page, String titlePath,
                             double score, String applicability, String excerpt) {
        Candidate merge(Candidate other) {
            return new Candidate(chunkId, revisionId == null ? other.revisionId : revisionId,
                documentTitle == null || documentTitle.equals("null") ? other.documentTitle : documentTitle,
                page == null ? other.page : page, titlePath == null || titlePath.equals("null") ? other.titlePath : titlePath,
                score + other.score, applicability, excerpt);
        }

        Evidence evidence() {
            return new Evidence(chunkId, revisionId, documentTitle, page, titlePath, score, applicability, excerpt);
        }
    }
}
