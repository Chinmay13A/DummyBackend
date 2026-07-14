package com.dummybackend.generatorservice.llm;

import com.dummybackend.generatorservice.exception.LlmGenerationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class ClaudeLlmProvider implements LlmProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LlmProvidersProperties.ProviderConfig config;

    public ClaudeLlmProvider(LlmProvidersProperties properties, ObjectMapper objectMapper) {
        this.config = properties.claude();
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("x-api-key", config.apiKey() != null ? config.apiKey() : "")
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String id() {
        return "claude";
    }

    @Override
    public String complete(String prompt) {
        requireApiKey();
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", config.model(),
                    "max_tokens", config.maxTokens(),
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String response = restClient.post()
                    .uri("/v1/messages")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return extractText(response);
        } catch (LlmGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmGenerationException("Failed to call Claude API", e);
        }
    }

    private String extractText(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode contentArray = root.get("content");

            if (contentArray == null || !contentArray.isArray() || contentArray.isEmpty()) {
                throw new LlmGenerationException("Unexpected Claude response shape: " + rawJson);
            }

            for (JsonNode block : contentArray) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText();
                }
            }

            throw new LlmGenerationException("No text block found in Claude response");
        } catch (LlmGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmGenerationException("Failed to parse Claude response envelope", e);
        }
    }

    private void requireApiKey() {
        if (!config.hasApiKey()) {
            throw new LlmGenerationException(
                    "Claude API key is not configured (set ANTHROPIC_API_KEY or llm.claude.api-key)");
        }
    }
}
