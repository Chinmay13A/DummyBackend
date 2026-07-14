package com.dummybackend.generatorservice.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SchemaValidationException.class)
    public ResponseEntity<ErrorResponse> handleSchemaValidation(SchemaValidationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid schema", ex.getErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request", errors));
    }

    @ExceptionHandler(LlmGenerationException.class)
    public ResponseEntity<ErrorResponse> handleLlmGeneration(LlmGenerationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("LLM generation failed", List.of(ex.getMessage())));
    }
}