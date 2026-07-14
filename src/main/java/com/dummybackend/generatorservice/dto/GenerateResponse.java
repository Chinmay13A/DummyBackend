package com.dummybackend.generatorservice.dto;

import java.util.List;
import java.util.Map;

public record GenerateResponse(int count, List<Map<String, Object>>data) { }
