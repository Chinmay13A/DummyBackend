package com.dummybackend.generatorservice.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProvidersProperties(
        String defaultProvider,
        ProviderConfig openai,
        ProviderConfig claude,
        ProviderConfig grok,
        ProviderConfig groq
) {
    public record ProviderConfig(
            String baseUrl,
            String apiKey,
            String model,
            int maxTokens
    ) {
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
