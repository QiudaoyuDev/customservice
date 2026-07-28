package com.hardwareai.support.knowledge;

import com.hardwareai.support.config.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

/** Calls the local BGE-compatible rerank service; caller retains deterministic fallback ordering. */
@Component
class LocalReranker {
    private final RestClient client;
    LocalReranker(AppProperties properties) { client = RestClient.builder().baseUrl(properties.rerankUrl()).build(); }

    @SuppressWarnings("unchecked")
    List<Integer> rank(String query, List<String> passages) {
        var response = client.post().uri("/v1/rerank").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("query", query, "documents", passages)).retrieve().body(Map.class);
        var rows = (List<Map<String, Object>>) response.get("results");
        return rows.stream().sorted((a, b) -> Double.compare(((Number)b.get("score")).doubleValue(), ((Number)a.get("score")).doubleValue()))
            .map(row -> ((Number)row.get("index")).intValue()).toList();
    }
}
