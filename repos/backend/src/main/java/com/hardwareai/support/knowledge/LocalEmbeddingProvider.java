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
 * BGE-compatible local embedding client with the shared external timeout policy.
 */
@Component
class LocalEmbeddingProvider implements EmbeddingProvider {
    private static final Logger log = LoggerFactory.getLogger(LocalEmbeddingProvider.class);
    private final RestClient client;
    private final String endpoint;

    LocalEmbeddingProvider(AppProperties properties, ExternalRestClientFactory clients) {
        endpoint = properties.embeddingUrl();
        client = clients.create(endpoint);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Double> embed(String text) {
        long start = System.nanoTime();
        try {
            var body = client.post().uri("/v1/embeddings").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("input", List.of(text), "normalize", true)).retrieve().body(Map.class);
            var vector = (List<Double>)((Map<?, ?>)((List<?>)body.get("data")).getFirst()).get("embedding");
            log.debug("Embedding ok url={} dims={} in {}ms", endpoint, vector.size(), elapsed(start));
            return vector;
        } catch (Exception e) {
            log.error("Embedding failed url={}", endpoint);
            throw e;
        }
    }

    private static long elapsed(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
