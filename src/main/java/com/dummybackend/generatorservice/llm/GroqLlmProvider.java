package com.dummybackend.generatorservice.llm;

import com.dummybackend.generatorservice.exception.LlmGenerationException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GroqLlmProvider implements LlmProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LlmProvidersProperties.ProviderConfig config;

    public GroqLlmProvider(LlmProvidersProperties properties, ObjectMapper objectMapper) {
        this.config = properties.groq();
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", "Bearer " + (config.apiKey() != null ? config.apiKey() : ""))
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String id() {
        return "groq";
    }

    @Override
    public String complete(String prompt) {
        requireApiKey();
        return ChatCompletionsSupport.complete(
                restClient, objectMapper, config.model(), config.maxTokens(), prompt, "Groq");
    }

    private void requireApiKey() {
        if (!config.hasApiKey()) {
            throw new LlmGenerationException(
                    "Groq API key is not configured (set GROQ_API_KEY or llm.groq.api-key)");
        }
    }
}
