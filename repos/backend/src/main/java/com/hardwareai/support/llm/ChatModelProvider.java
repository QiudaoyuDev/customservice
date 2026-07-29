package com.hardwareai.support.llm;

/** Provider-neutral OpenAI-compatible chat contract used by orchestration and configuration tests. */
public interface ChatModelProvider {
    String complete(String baseUrl, String apiKey, String model, String system, String prompt);

    String complete(String baseUrl, String apiKey, String model, String system, String prompt, double temperature, int maxTokens);

    boolean testConnection(String baseUrl, String apiKey);
}
