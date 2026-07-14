package com.dummybackend.generatorservice.exception;

public class UnknownLlmProviderException extends RuntimeException {

    public UnknownLlmProviderException(String provider) {
        super("Unknown LLM provider: \"" + provider + "\". Supported: openai, claude, grok, groq");
    }
}
