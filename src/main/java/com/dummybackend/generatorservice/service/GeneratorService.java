package com.dummybackend.generatorservice.service;

import com.dummybackend.generatorservice.dto.GenerateRequest;
import com.dummybackend.generatorservice.dto.GenerateResponse;
import com.dummybackend.generatorservice.exception.LlmGenerationException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeneratorService {

    private final RestClient anthropicRestClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.model}")
    private String model;

    @Value("${anthropic.max-tokens}")
    private int maxTokens;

    public GeneratorService(RestClient anthropicRestClient,
                             PromptBuilder promptBuilder,
                             ObjectMapper objectMapper) {
        this.anthropicRestClient = anthropicRestClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    public GenerateResponse generate(GenerateRequest request) {
        int count = (request.count() != null) ? request.count() : 1;
        String prompt = promptBuilder.build(request.schema(), count);

        String rawResponse = callLlm(prompt);
        List<Map<String, Object>> data = parseLlmOutput(rawResponse);

        return new GenerateResponse(data.size(), data);
    }

    private String callLlm(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String response = anthropicRestClient.post()
                    .uri("/v1/messages")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return extractTextFromAnthropicResponse(response);

        } catch (Exception e) {
            throw new LlmGenerationException("Failed to call LLM API", e);
        }
    }

    private String extractTextFromAnthropicResponse(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode contentArray = root.get("content");

            if (contentArray == null || !contentArray.isArray() || contentArray.isEmpty()) {
                throw new LlmGenerationException("Unexpected LLM response shape: " + rawJson);
            }

            // find the first text block
            for (JsonNode block : contentArray) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText();
                }
            }

            throw new LlmGenerationException("No text block found in LLM response");

        } catch (LlmGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmGenerationException("Failed to parse LLM response envelope", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseLlmOutput(String text) {
        try {
            String cleaned = text.trim()
                    .replaceAll("^```json", "")
                    .replaceAll("^```", "")
                    .replaceAll("```$", "")
                    .trim();

            return objectMapper.readValue(cleaned, List.class);

        } catch (Exception e) {
            throw new LlmGenerationException("LLM returned invalid JSON: " + e.getMessage(), e);
        }
    }
}