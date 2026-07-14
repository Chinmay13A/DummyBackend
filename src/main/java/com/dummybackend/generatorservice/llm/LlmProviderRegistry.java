package com.dummybackend.generatorservice.llm;

import com.dummybackend.generatorservice.exception.UnknownLlmProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LlmProviderRegistry {

    private final Map<String, LlmProvider> providersById;
    private final String defaultProvider;

    public LlmProviderRegistry(
            List<LlmProvider> providers,
            @Value("${llm.default-provider:openai}") String defaultProvider) {
        this.providersById = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        p -> p.id().toLowerCase(),
                        Function.identity()));
        this.defaultProvider = defaultProvider.toLowerCase();
    }

    public LlmProvider resolve(String provider) {
        String id = (provider == null || provider.isBlank())
                ? defaultProvider
                : provider.trim().toLowerCase();

        LlmProvider resolved = providersById.get(id);
        if (resolved == null) {
            throw new UnknownLlmProviderException(provider);
        }
        return resolved;
    }
}
