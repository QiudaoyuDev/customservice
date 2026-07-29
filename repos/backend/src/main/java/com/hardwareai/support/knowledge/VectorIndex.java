package com.hardwareai.support.knowledge;

import com.hardwareai.support.config.AppProperties;
import com.hardwareai.support.config.ExternalRestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Qdrant adapter: application metadata is always embedded in the payload for retrieval filtering.
 */
@Service
class VectorIndex implements VectorStoreAdapter {

    private static final Logger log = LoggerFactory.getLogger(VectorIndex.class);

    private final RestClient client;
    private final EmbeddingProvider embedding;
    private final AppProperties config;
    private final KnowledgeRevisionApplicabilityRepository applicability;

    VectorIndex(AppProperties config, ExternalRestClientFactory clients, KnowledgeRevisionApplicabilityRepository applicability,
                EmbeddingProvider embedding) {
        this.config = config;
        client = clients.create(config.qdrantUrl(), "api-key", config.qdrantApiKey());
        this.embedding = embedding;
        this.applicability = applicability;
    }

    @Override
    public void upsert(KnowledgeRevision revision, KnowledgeDocument document, List<KnowledgeChunk> chunks) {
        if (chunks.isEmpty()) throw new IllegalStateException("Cannot index a revision without chunks");
        long start = System.nanoTime();
        try {
            var scope = applicability.findAllByRevisionId(revision.id()).stream().findFirst().orElse(null);
            var vector = embedding.embed(chunks.getFirst().content());
            ensureCollection(vector.size());
            client
                    .put()
                    .uri("/collections/{collection}/points?wait=true", config.qdrantCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                            "points", chunks.stream().map(chunk -> {
                                        var payload = new java.util.HashMap<String, Object>();
                                        payload.put("revisionId", revision.id().toString());
                                        payload.put("chunkNo", chunk.chunkNo());
                                        payload.put("productModelId", revision.productModelId().toString());
                                        payload.put("tenantId", document.tenantId().toString());
                                        payload.put("region", revision.region());
                                        payload.put("locale", document.locale());
                                        payload.put("status", "PUBLISHED");
                                        payload.put("indexStatus", "READY");
                                        payload.put("indexVersion", 1);
                                        payload.put("source", chunk.sourceLabel());
                                        payload.put("titlePath", chunk.titlePath());
                                        payload.put("page", chunk.pageFrom());
                                        payload.put("text", chunk.content());
                                        if (scope != null && scope.productVariantId() != null) payload.put("productVariantId", scope.productVariantId().toString());
                                        if (scope != null && scope.hardwareRevision() != null) payload.put("hardwareRevision", scope.hardwareRevision());
                                        payload.put("firmwareScoped", scope != null && (scope.firmwareMin() != null || scope.firmwareMax() != null));
                                        return Map.of("id", chunk.id().toString(), "vector", embedding.embed(chunk.content()), "payload", payload);
                                    }).toList()
                            )
                    )
                    .retrieve()
                    .toBodilessEntity();
            if (!smokeQuery(revision.id(), chunks.getFirst().content())) {
                throw new IllegalStateException("Qdrant index smoke query did not return the indexed revision");
            }
            log.info("Qdrant upsert ok collection={} revision={} chunks={} in {}ms",
                    config.qdrantCollection(), revision.id(), chunks.size(), millis(start));
        } catch (Exception e) {
            log.error("Qdrant upsert failed collection={} revision={}", config.qdrantCollection(), revision.id());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean smokeQuery(UUID revisionId, String text) {
        var response = client.post().uri("/collections/{collection}/points/query", config.qdrantCollection())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", embedding.embed(text), "limit", 1, "with_payload", false,
                        "filter", Map.of("must", List.of(Map.of("key", "revisionId", "match", Map.of("value", revisionId.toString()))))))
                .retrieve().body(Map.class);
        if (response == null || !(response.get("result") instanceof Map<?, ?> result)) return false;
        return result.get("points") instanceof List<?> points && !points.isEmpty();
    }

    /**
     * Removes every chunk for a revision so a new request cannot retrieve deprecated knowledge.
     */
    @Override
    public void removeRevision(UUID revisionId) {
        long start = System.nanoTime();
        try {
            client.post().uri("/collections/{collection}/points/delete?wait=true", config.qdrantCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("filter", Map.of("must", List.of(Map.of("key", "revisionId", "match", Map.of("value", revisionId.toString()))))))
                    .retrieve().toBodilessEntity();
            log.info("Qdrant delete ok collection={} revision={} in {}ms", config.qdrantCollection(), revisionId, millis(start));
        } catch (Exception e) {
            log.error("Qdrant delete failed collection={} revision={}", config.qdrantCollection(), revisionId);
            throw e;
        }
    }

    /**
     * Creates the collection lazily because embedding dimensions are model-dependent.
     */
    private void ensureCollection(int vectorSize) {
        boolean exists = client
                .get()
                .uri("/collections/{collection}", config.qdrantCollection())
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful());
        if (!exists) {
            long start = System.nanoTime();
            client
                    .put()
                    .uri("/collections/{collection}", config.qdrantCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("vectors", Map.of("size", vectorSize, "distance", "Cosine")))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Qdrant collection created collection={} dim={} in {}ms", config.qdrantCollection(), vectorSize, millis(start));
        }
    }

    private static long millis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
