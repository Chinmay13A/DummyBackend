package com.dummybackend.generatorservice.exception;

import java.util.List;

public record ErrorResponse(String message, List<String> details) {}