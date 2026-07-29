package com.hardwareai.support.knowledge;

import com.hardwareai.support.config.AppProperties;
import com.hardwareai.support.config.ExternalRestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Calls the local BGE-compatible rerank service; caller retains deterministic fallback ordering.
 */
@Component
public class LocalReranker implements RerankProvider {
    private static final Logger log = LoggerFactory.getLogger(LocalReranker.class);

    private final RestClient client;
    private final String rerankUrl;

    LocalReranker(AppProperties properties, ExternalRestClientFactory clients) {
        this.rerankUrl = properties.rerankUrl();
        client = clients.create(rerankUrl);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> rank(String query, List<String> passages) {
        long start = System.nanoTime();
        try {
            var response = client.post().uri("/v1/rerank").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", query, "documents", passages)).retrieve().body(Map.class);
            var rows = (List<Map<String, Object>>)response.get("results");
            var order = rows.stream()
                .sorted((a, b) -> Double.compare(((Number)b.get("score")).doubleValue(), ((Number)a.get("score")).doubleValue()))
                .map(row -> ((Number)row.get("index")).intValue()).toList();
            log.debug("Rerank ok url={} passages={} in {}ms", rerankUrl, passages.size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return order;
        } catch (Exception e) {
            log.warn("Rerank unavailable url={} (falling back to retrieval order)", rerankUrl);
            throw e;
        }
    }
}
