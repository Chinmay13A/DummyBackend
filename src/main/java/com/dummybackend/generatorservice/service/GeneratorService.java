package com.dummybackend.generatorservice.service;

import com.dummybackend.generatorservice.dto.GenerateRequest;
import com.dummybackend.generatorservice.dto.GenerateResponse;
import com.dummybackend.generatorservice.exception.LlmGenerationException;
import com.dummybackend.generatorservice.llm.LlmProvider;
import com.dummybackend.generatorservice.llm.LlmProviderRegistry;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        LlmProvider provider = llmProviderRegistry.resolve(request.provider());
        String prompt = promptBuilder.build(request.schema(), count);

        String rawResponse = provider.complete(prompt);
        List<Map<String, Object>> data = parseLlmOutput(rawResponse);
        data = enforceCount(data, count, request.schema(), provider, true);

        return new GenerateResponse(count, data);
    }

    private List<Map<String, Object>> enforceCount(
            List<Map<String, Object>> data,
            int count,
            Map<String, Object> schema,
            LlmProvider provider,
            boolean allowRetry) {
        if (data.size() > count) {
            return new ArrayList<>(data.subList(0, count));
        }

        if (data.size() < count) {
            if (!allowRetry) {
                throw new LlmGenerationException(
                        "LLM returned " + data.size() + " records but " + count + " were requested");
            }

            String retryPrompt = promptBuilder.buildRetry(schema, count, data.size());
            List<Map<String, Object>> retried = parseLlmOutput(provider.complete(retryPrompt));
            return enforceCount(retried, count, schema, provider, false);
        }

        return data;
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
