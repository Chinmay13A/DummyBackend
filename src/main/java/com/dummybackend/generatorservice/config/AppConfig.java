package com.dummybackend.generatorservice.config;

import com.dummybackend.generatorservice.llm.LlmProvidersProperties;
import com.dummybackend.generatorservice.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({LlmProvidersProperties.class, RateLimitProperties.class})
public class AppConfig {
}
