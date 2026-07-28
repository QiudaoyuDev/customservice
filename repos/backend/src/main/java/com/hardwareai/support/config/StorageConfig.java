package com.hardwareai.support.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class StorageConfig {

    @Bean
    MinioClient minioClient(AppProperties p) {
        return MinioClient.builder()
            .endpoint(p.storage().endpoint())
            .credentials(p.storage().accessKey(), p.storage().secretKey())
            .build();
    }
}
