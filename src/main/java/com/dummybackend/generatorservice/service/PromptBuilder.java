package com.dummybackend.generatorservice.service;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptBuilder {

    private final ObjectMapper objectMapper;

    public PromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String build(Map<String, Object> schema, int count) {
        String schemaJson;
        try {
            schemaJson = objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize schema", e);
        }

        return """
            You are a dummy data generator.

            Given the following JSON schema, generate exactly %d realistic data records that match it.

            Schema:
            %s

            Rules:
            - Infer the semantic meaning of each field from its name (e.g. "email" should be a valid email, "dob" a plausible past date).
            - Respect the declared "type" for each field strictly.
            - Respect any "min", "max", "values" (for enum), or "description" constraints exactly.
            - Return ONLY a valid JSON array of %d objects. No explanations, no markdown, no code fences.
            - Each object in the array must have exactly the same keys as the schema.
            """.formatted(count, schemaJson, count);
    }
}