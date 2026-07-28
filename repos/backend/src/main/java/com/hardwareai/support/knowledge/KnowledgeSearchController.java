package com.hardwareai.support.knowledge;

import com.hardwareai.support.common.CurrentUser;
import com.hardwareai.support.config.AppProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Admin-only retrieval probe; it proves applicability filters before conversational AI is introduced.
 */
@RestController
@RequestMapping("/api/search")
class KnowledgeSearchController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchController.class);

    private final RestClient qdrant;
    private final RestClient embedding;
    private final AppProperties config;
    private final CurrentUser current;
    private final KnowledgeChunkRepository chunks;
    private final LocalReranker reranker;

    KnowledgeSearchController(AppProperties config, CurrentUser current, KnowledgeChunkRepository chunks, LocalReranker reranker) {
        this.config = config;
        this.current = current;
        qdrant = RestClient.builder()
                .baseUrl(config.qdrantUrl())
                .defaultHeader("api-key", config.qdrantApiKey())
                .build();
        embedding = RestClient.create(config.embeddingUrl());
        this.chunks = chunks;
        this.reranker = reranker;
    }

    @PostMapping
    public Map<String, Object> search(@Valid @RequestBody Query query) {
        long start = System.nanoTime();
        var vector = embed(query.query());
        var filter = Map.of(
                "must",
                List.of(
                        Map.of(
                                "key", "tenantId", "match", Map.of("value", current.tenantId().toString())
                        ),
                        Map.of(
                                "key",
                                "productModelId",
                                "match",
                                Map.of("value", query.productModelId().toString())
                        ),
                        Map.of("key", "region", "match", Map.of("value", query.region())),
                        Map.of("key", "locale", "match", Map.of("value", query.locale())),
                        Map.of("key", "status", "match", Map.of("value", "PUBLISHED"))
                )
        );
        var vectorResult = qdrant
                .post()
                .uri("/collections/{collection}/points/query", config.qdrantCollection())
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        Map.of(
                                "query",
                                vector,
                                "limit",
                                Math.min(query.limit(), 10),
                                "with_payload",
                                true,
                                "filter",
                                filter
                        )
                )
                .retrieve()
                .body(Map.class);
        var keywordResult = chunks.keywordSearch(current.tenantId(), query.productModelId(), query.region(), query.locale(), query.query(), PageRequest.of(0, Math.min(query.limit(), 10)));
        var merged = new LinkedHashMap<String, Result>();
        for (var chunk : keywordResult) merged.put(chunk.id().toString(), Result.keyword(chunk));
        addVectorResults(merged, vectorResult);
        var results = new ArrayList<>(merged.values());
        results.sort(java.util.Comparator.comparingDouble(Result::score).reversed());
        boolean reranked = false;
        try {
            var original = List.copyOf(results);
            results = reranker.rank(query.query(), original.stream().map(Result::text).toList()).stream()
                    .filter(index -> index >= 0 && index < original.size()).map(original::get).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            reranked = true;
        } catch (Exception ignored) {
            // Retrieval remains available when the optional local rerank adapter is unavailable.
        }
        log.info("Search tenant={} product={} region={} locale={} queryLen={} hits={} reranked={} in {}ms",
                current.tenantId(), query.productModelId(), query.region(), query.locale(),
                query.query().length(), results.size(), reranked,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        return Map.of("results", results.stream().limit(query.limit()).toList());
    }

    @SuppressWarnings("unchecked")
    private void addVectorResults(Map<String, Result> merged, Map<?, ?> response) {
        var result = response == null ? null : (Map<?, ?>) response.get("result");
        Object rawPoints = result == null ? null : result.get("points");
        var points = rawPoints instanceof List<?> list ? list : List.of();
        for (Object value : points) {
            if (!(value instanceof Map<?, ?> point)) continue;
            String id = String.valueOf(point.get("id"));
            double score = point.get("score") instanceof Number n ? n.doubleValue() : 0;
            var payload = point.get("payload") instanceof Map<?, ?> p ? p : Map.of();
            Result vector = new Result(id, String.valueOf(payload.get("source")), String.valueOf(payload.get("text")),
                    String.valueOf(payload.get("revisionId")), payload.get("chunkNo"), score, true, false);
            merged.merge(id, vector, Result::merge);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Double> embed(String text) {
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
            log.debug("Embedding ok url={} dims={} in {}ms", config.embeddingUrl(), vector.size(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return vector;
        } catch (Exception e) {
            log.error("Embedding failed url={}: {}", config.embeddingUrl(), e.getMessage(), e);
            throw e;
        }
    }

    record Query(
            @NotBlank @Size(max = 1000) String query,
            @NotNull UUID productModelId,
            @NotBlank @Size(max = 16) String region,
            @NotBlank @Size(max = 16) String locale,
            @Min(1) @Max(10) int limit
    ) {
    }

    /**
     * Stable API shape used by admins and later by the RAG orchestrator.
     */
    record Result(String chunkId, String source, String text, String revisionId, Object chunkNo, double score,
                  boolean vector, boolean keyword) {
        static Result keyword(KnowledgeChunk c) {
            return new Result(c.id().toString(), c.sourceLabel(), c.content(), "", c.chunkNo(), 1.0, false, true);
        }

        Result merge(Result other) {
            return new Result(chunkId, source, text, revisionId.isBlank() ? other.revisionId : revisionId, chunkNo, score + other.score, vector || other.vector, keyword || other.keyword);
        }
    }
}
