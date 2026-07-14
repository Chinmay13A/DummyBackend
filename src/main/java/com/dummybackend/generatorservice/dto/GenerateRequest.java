package com.dummybackend.generatorservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record GenerateRequest(
        String provider,

        @Min(value = 1, message = "count must be least 1")
        @Max(value = 50, message = "count must not exceed 50")
        Integer count,

        @NotEmpty(message = "schema must not be empty")
        Map<String, Object> schema
) { }
