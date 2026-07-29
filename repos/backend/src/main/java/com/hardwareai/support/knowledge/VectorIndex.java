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
class VectorIndex {

    private static final Logger log = LoggerFactory.getLogger(VectorIndex.class);

    private final RestClient client;
    private final RestClient embedding;
    private final AppProperties config;

    VectorIndex(AppProperties config, ExternalRestClientFactory clients) {
        this.config = config;
        client = clients.create(config.qdrantUrl(), "api-key", config.qdrantApiKey());
        embedding = clients.create(config.embeddingUrl());
    }

    void upsert(KnowledgeRevision revision, KnowledgeDocument document, List<KnowledgeChunk> chunks) {
        if (chunks.isEmpty()) throw new IllegalStateException("Cannot index a revision without chunks");
        long start = System.nanoTime();
        try {
            var vector = embedding(chunks.getFirst().content());
            ensureCollection(vector.size());
            client
                    .put()
                    .uri("/collections/{collection}/points?wait=true", config.qdrantCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "points", chunks.stream().map(chunk -> Map.of(
                                            "id", chunk.id().toString(), "vector", embedding(chunk.content()),
                                            "payload", Map.of(
                                                    "revisionId", revision.id().toString(), "chunkNo", chunk.chunkNo(),
                                                    "productModelId", revision.productModelId().toString(), "tenantId", document.tenantId().toString(),
                                                    "region", revision.region(), "locale", document.locale(), "status", revision.status().name(),
                                                    "source", chunk.sourceLabel(), "text", chunk.content()
                                            )
                                    )).toList()
                            )
                    )
                    .retrieve()
                    .toBodilessEntity();
            log.info("Qdrant upsert ok collection={} revision={} chunks={} in {}ms",
                    config.qdrantCollection(), revision.id(), chunks.size(), millis(start));
        } catch (Exception e) {
            log.error("Qdrant upsert failed collection={} revision={}", config.qdrantCollection(), revision.id());
            throw e;
        }
    }

    /**
     * Removes every chunk for a revision so a new request cannot retrieve deprecated knowledge.
     */
    void removeRevision(UUID revisionId) {
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

    @SuppressWarnings("unchecked")
    private List<Double> embedding(String text) {
        long start = System.nanoTime();
        try {
            var body = embedding
                    .post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("input", List.of(text), "normalize", true))
                    .retrieve()
                    .body(Map.class);
            var vector = (List<Double>) ((Map<?, ?>) ((List<?>) body.get("data")).getFirst()).get("embedding");
            log.debug("Embedding ok url={} dims={} in {}ms", config.embeddingUrl(), vector.size(), millis(start));
            return vector;
        } catch (Exception e) {
            log.error("Embedding failed url={}", config.embeddingUrl());
            throw e;
        }
    }

    private static long millis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
