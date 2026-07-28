package com.hardwareai.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
/** External configuration; credentials are supplied only through environment variables. */
public record AppProperties(Security security, Storage storage, Qr qr, Bootstrap bootstrap) {
  public record Security(String jwtSecret) {}
  public record Storage(String endpoint, String accessKey, String secretKey, String bucket) {}
  public record Qr(String secret) {}
  public record Bootstrap(String adminEmail, String adminPassword, String tenantName) {}
}
