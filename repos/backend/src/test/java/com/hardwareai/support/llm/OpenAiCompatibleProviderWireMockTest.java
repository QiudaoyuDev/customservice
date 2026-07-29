package com.hardwareai.support.llm;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.hardwareai.support.config.AppProperties;
import com.hardwareai.support.config.ExternalRestClientFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiCompatibleProviderWireMockTest {
    private WireMockServer upstream;

    @BeforeEach
    void startServer() {
        upstream = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        upstream.start();
    }

    @AfterEach
    void stopServer() {
        upstream.stop();
    }

    @Test
    void sendsTheOpenAiCompatibleRequestWithoutExposingTheCredentialInTheResponsePath() {
        upstream.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer upstream-api-key"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
                .willReturn(okJson("{\"choices\":[{\"message\":{\"content\":\"grounded reply\"}}]}")));
        var provider = new OpenAiCompatibleProvider(new ExternalRestClientFactory(properties()));

        var reply = provider.complete(upstream.baseUrl(), "upstream-api-key", "test-model", "system", "prompt");

        assertEquals("grounded reply", reply);
        upstream.verify(postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }

    private static AppProperties properties() {
        return new AppProperties(
                new AppProperties.Security("x".repeat(32)),
                new AppProperties.Storage("http://storage", "access", "secret", "bucket"),
                new AppProperties.Qr("x".repeat(32)),
                new AppProperties.Bootstrap("admin@example.test", "password", "tenant"),
                new AppProperties.Llm(false, "", "", ""),
                new AppProperties.ModelKeyEncryption("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", "v1"),
                new AppProperties.ExternalClients(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1)),
                "http://embedding", "http://ocr", "http://rerank", "http://qdrant", "qdrant-key", "chunks");
    }
}
