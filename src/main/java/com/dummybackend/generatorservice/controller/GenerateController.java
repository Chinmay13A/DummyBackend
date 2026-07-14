package com.dummybackend.generatorservice.controller;

import com.dummybackend.generatorservice.dto.GenerateRequest;
import com.dummybackend.generatorservice.dto.GenerateResponse;
import com.dummybackend.generatorservice.service.GeneratorService;
import com.dummybackend.generatorservice.service.SchemaValidator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GenerateController {

    @Autowired
    SchemaValidator schemaValidator;

    @Autowired
    GeneratorService generatorService;

    @PostMapping("/generate")
    public GenerateResponse generateResponse(@Valid @RequestBody GenerateRequest body){
        schemaValidator.validate(body.schema());
        return generatorService.generate(body);
    }
}
