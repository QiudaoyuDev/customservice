package com.hardwareai.support.config;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class StorageConfig {

    @Bean
    MinioClient minioClient(AppProperties p) {
        var timeouts = p.externalClients();
        var httpClient = new OkHttpClient.Builder()
            .connectTimeout(timeouts.connectTimeout())
            .readTimeout(timeouts.readTimeout())
            .callTimeout(timeouts.requestTimeout())
            .build();
        return MinioClient.builder()
            .endpoint(p.storage().endpoint())
            .credentials(p.storage().accessKey(), p.storage().secretKey())
            .httpClient(httpClient)
            .build();
    }
}
