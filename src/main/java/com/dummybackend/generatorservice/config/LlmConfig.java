package com.dummybackend.generatorservice.config;

import com.dummybackend.generatorservice.llm.LlmProvidersProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmProvidersProperties.class)
public class LlmConfig {
}
