package com.dummybackend.generatorservice.llm;

import com.dummybackend.generatorservice.exception.LlmGenerationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Shared request/parse helpers for OpenAI-compatible chat completions APIs
 * (OpenAI, xAI/Grok, etc.).
 */
final class ChatCompletionsSupport {

    private ChatCompletionsSupport() {}

    static String complete(
            RestClient restClient,
            ObjectMapper objectMapper,
            String model,
            int maxTokens,
            String prompt,
            String providerLabel) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return extractText(objectMapper, response, providerLabel);
        } catch (LlmGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmGenerationException("Failed to call " + providerLabel + " API", e);
        }
    }

    static String extractText(ObjectMapper objectMapper, String rawJson, String providerLabel) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode choices = root.get("choices");

            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new LlmGenerationException(
                        "Unexpected " + providerLabel + " response shape: " + rawJson);
            }

            String content = choices.get(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new LlmGenerationException(
                        "No message content found in " + providerLabel + " response");
            }
            return content;
        } catch (LlmGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmGenerationException(
                    "Failed to parse " + providerLabel + " response envelope", e);
        }
    }
}
