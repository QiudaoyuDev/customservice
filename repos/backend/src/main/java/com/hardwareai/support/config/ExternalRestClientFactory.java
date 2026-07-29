package com.hardwareai.support.config;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Creates every blocking external HTTP client with one bounded timeout policy.
 * The effective response limit is the stricter of the read and whole-request limits.
 */
@Component
public class ExternalRestClientFactory {

    private final AppProperties.ExternalClients timeouts;

    public ExternalRestClientFactory(AppProperties properties) {
        this.timeouts = properties.externalClients();
    }

    public RestClient create(String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()).build();
    }

    public RestClient create(String baseUrl, String headerName, String headerValue) {
        return RestClient.builder().baseUrl(baseUrl).defaultHeader(headerName, headerValue)
            .requestFactory(requestFactory()).build();
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeouts.connectTimeout());
        factory.setReadTimeout(timeouts.effectiveResponseTimeout());
        return factory;
    }
}
