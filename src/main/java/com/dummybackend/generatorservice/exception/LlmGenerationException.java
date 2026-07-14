package com.dummybackend.generatorservice.exception;

public class LlmGenerationException extends RuntimeException {
    public LlmGenerationException(String message) {
        super(message);
    }

    public LlmGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}