package com.hardwareai.support.config;

import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * Exposes dependency reachability without leaking URLs, credentials or response bodies.
 */
@Component("externalDependencies")
class ExternalDependencyHealthIndicator implements HealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(ExternalDependencyHealthIndicator.class);

    private final AppProperties properties;
    private final MinioClient minio;

    private final ExternalRestClientFactory clients;

    ExternalDependencyHealthIndicator(AppProperties properties, MinioClient minio, ExternalRestClientFactory clients) {
        this.properties = properties;
        this.minio = minio;
        this.clients = clients;
    }

    @Override
    public Health health() {
        boolean allUp = true;
        allUp &= probe("MinIO", () -> {
            minio.bucketExists(io.minio.BucketExistsArgs.builder().bucket(properties.storage().bucket()).build());
            return null;
        });
        allUp &= probeHttp("OCR", properties.ocrUrl(), "/health");
        allUp &= probeHttp("Embedding", properties.embeddingUrl(), "/health");
        allUp &= probeHttp("Qdrant", properties.qdrantUrl(), "/healthz");
        return allUp
            ? Health.up().withDetail("dependencies", "reachable").build()
            : Health.down().withDetail("dependency", "unavailable").build();
    }

    private boolean probeHttp(String name, String baseUrl, String path) {
        return probe(name, () -> {
            clients.create(baseUrl).get().uri(path).retrieve().toBodilessEntity();
            return null;
        });
    }

    private boolean probe(String name, Callable<Void> call) {
        try {
            call.call();
            log.debug("Dependency up: {}", name);
            return true;
        } catch (Exception e) {
            log.warn("Dependency down: {}", name);
            return false;
        }
    }
}
