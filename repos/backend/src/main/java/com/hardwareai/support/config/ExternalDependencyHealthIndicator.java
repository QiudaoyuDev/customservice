package com.hardwareai.support.config;

import io.minio.MinioClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Exposes dependency reachability without leaking URLs, credentials or response bodies. */
@Component("externalDependencies")
class ExternalDependencyHealthIndicator implements HealthIndicator {
    private final AppProperties properties;
    private final MinioClient minio;

    ExternalDependencyHealthIndicator(AppProperties properties, MinioClient minio) {
        this.properties = properties;
        this.minio = minio;
    }

    @Override
    public Health health() {
        try {
            minio.bucketExists(io.minio.BucketExistsArgs.builder().bucket(properties.storage().bucket()).build());
            checkHttp(properties.ocrUrl(), "/health");
            checkHttp(properties.embeddingUrl(), "/health");
            checkHttp(properties.qdrantUrl(), "/healthz");
            return Health.up().withDetail("dependencies", "reachable").build();
        } catch (Exception e) {
            return Health.down().withDetail("dependency", "unavailable").build();
        }
    }

    private void checkHttp(String baseUrl, String path) {
        RestClient.builder().baseUrl(baseUrl).build().get().uri(path).retrieve().toBodilessEntity();
    }
}
