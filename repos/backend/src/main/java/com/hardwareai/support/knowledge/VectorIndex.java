package com.hardwareai.support.knowledge;

import com.hardwareai.support.config.AppProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Qdrant adapter: application metadata is always embedded in the payload for retrieval filtering.
 */
@Service
class VectorIndex {

    private final RestClient client;
    private final AppProperties config;

    VectorIndex(AppProperties config) {
        this.config = config;
        client = RestClient.builder()
            .baseUrl(config.qdrantUrl())
            .defaultHeader("api-key", config.qdrantApiKey())
            .build();
    }

    void upsert(KnowledgeRevision revision, KnowledgeDocument document, List<KnowledgeChunk> chunks) {
        if (chunks.isEmpty()) throw new IllegalStateException("Cannot index a revision without chunks");
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
    }

    /** Removes every chunk for a revision so a new request cannot retrieve deprecated knowledge. */
    void removeRevision(UUID revisionId) {
        client.post().uri("/collections/{collection}/points/delete?wait=true", config.qdrantCollection())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("filter", Map.of("must", List.of(Map.of("key", "revisionId", "match", Map.of("value", revisionId.toString()))))))
            .retrieve().toBodilessEntity();
    }

    /**
     * Creates the collection lazily because embedding dimensions are model-dependent.
     */
    private void ensureCollection(int vectorSize) {
        boolean exists = client
            .get()
            .uri("/collections/{collection}", config.qdrantCollection())
            .exchange((request, response) -> response.getStatusCode().is2xxSuccessful());
        if (!exists) client
            .put()
            .uri("/collections/{collection}", config.qdrantCollection())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("vectors", Map.of("size", vectorSize, "distance", "Cosine")))
            .retrieve()
            .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private List<Double> embedding(String text) {
        var body = RestClient.create(config.embeddingUrl())
            .post()
            .uri("/v1/embeddings")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("input", List.of(text), "normalize", true))
            .retrieve()
            .body(Map.class);
        return (List<Double>)((Map<?, ?>)((List<?>)body.get("data")).getFirst()).get("embedding");
    }
}
