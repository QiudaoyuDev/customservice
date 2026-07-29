package com.hardwareai.support.llm;

import com.hardwareai.support.config.ExternalRestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Minimal OpenAI-compatible provider; credentials are supplied at call time and never logged.
 */
@Component
public class OpenAiCompatibleProvider implements ChatModelProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);
    private final ExternalRestClientFactory clients;

    public OpenAiCompatibleProvider(ExternalRestClientFactory clients) {
        this.clients = clients;
    }

    @Override
    public String complete(String baseUrl, String apiKey, String model, String system, String prompt) {
        return complete(baseUrl, apiKey, model, system, prompt, 0d, 800);
    }

    @Override
    public String complete(String baseUrl, String apiKey, String model, String system, String prompt, double temperature,
        int maxTokens) {
        long start = System.nanoTime();
        try {
            var response = clients.create(baseUrl, "Authorization", "Bearer " + apiKey).post().uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "temperature", temperature, "max_tokens", maxTokens, "messages",
                    List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", prompt))))
                .retrieve().body(Map.class);
            var choices = (List<?>)response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("LLM returned no choice baseUrl={} model={} in {}ms", baseUrl, model,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                throw new IllegalStateException("LLM returned no choice");
            }
            var content = String.valueOf(((Map<?, ?>)((Map<?, ?>)choices.getFirst()).get("message")).get("content"));
            log.info("LLM completion ok baseUrl={} model={} promptLen={} replyLen={} in {}ms", baseUrl, model,
                prompt.length(), content.length(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return content;
        } catch (Exception e) {
            log.error("LLM completion failed baseUrl={} model={} in {}ms", baseUrl, model,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            throw e;
        }
    }

    @Override
    public boolean testConnection(String baseUrl, String apiKey) {
        try {
            return clients.create(baseUrl, "Authorization", "Bearer " + apiKey).get().uri("/v1/models")
                .exchange((request, response) -> response.getStatusCode().is2xxSuccessful());
        } catch (Exception exception) {
            log.warn("LLM connection test failed baseUrl={}", baseUrl);
            return false;
        }
    }
}
