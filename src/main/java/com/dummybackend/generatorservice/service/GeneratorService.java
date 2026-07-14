package com.dummybackend.generatorservice.service;

import com.dummybackend.generatorservice.dto.GenerateRequest;
import com.dummybackend.generatorservice.dto.GenerateResponse;
import com.dummybackend.generatorservice.exception.LlmGenerationException;
import com.dummybackend.generatorservice.llm.LlmProviderRegistry;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GeneratorService {

    private final LlmProviderRegistry llmProviderRegistry;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public GeneratorService(
            LlmProviderRegistry llmProviderRegistry,
            PromptBuilder promptBuilder,
            ObjectMapper objectMapper) {
        this.llmProviderRegistry = llmProviderRegistry;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    public GenerateResponse generate(GenerateRequest request) {
        int count = (request.count() != null) ? request.count() : 1;
        String prompt = promptBuilder.build(request.schema(), count);

        String rawResponse = llmProviderRegistry
                .resolve(request.provider())
                .complete(prompt);

        List<Map<String, Object>> data = parseLlmOutput(rawResponse);
        return new GenerateResponse(data.size(), data);
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
