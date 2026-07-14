package com.dummybackend.generatorservice.llm;

public interface LlmProvider {

    String id();

    /**
     * Sends {@code prompt} to the provider and returns plain assistant text only.
     */
    String complete(String prompt);
}
