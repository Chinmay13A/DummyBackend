package com.dummybackend.generatorservice.service;

import com.dummybackend.generatorservice.exception.SchemaValidationException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SchemaValidator {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("string", "integer", "float", "boolean", "date", "enum");

    private static final int MAX_FIELDS = 30;

    @SuppressWarnings("unchecked")
    public void validate(Map<String, Object> schema) {
        List<String> errors = new ArrayList<>();

        if (schema.size() > MAX_FIELDS) {
            errors.add("schema cannot have more than " + MAX_FIELDS + " fields");
        }

        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldDef = entry.getValue();

            Map<String, Object> normalized = normalize(fieldDef);
            Object typeObj = normalized.get("type");

            if (!(typeObj instanceof String type) || !ALLOWED_TYPES.contains(type)) {
                errors.add("field \"" + fieldName + "\" has unsupported or missing type: " + typeObj);
                continue;
            }

            if (type.equals("enum")) {
                Object values = normalized.get("values");
                if (!(values instanceof List<?> list) || list.isEmpty()) {
                    errors.add("field \"" + fieldName + "\" is type enum but missing non-empty \"values\" array");
                }
            }

            if ((type.equals("integer") || type.equals("float")) ) {
                Object min = normalized.get("min");
                Object max = normalized.get("max");
                if (min instanceof Number minNum && max instanceof Number maxNum
                        && minNum.doubleValue() > maxNum.doubleValue()) {
                    errors.add("field \"" + fieldName + "\": min cannot be greater than max");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new SchemaValidationException(errors);
        }
    }

    // supports shorthand: "age": "integer"  →  { "type": "integer" }
    @SuppressWarnings("unchecked")
    private Map<String, Object> normalize(Object fieldDef) {
        if (fieldDef instanceof String type) {
            return Map.of("type", type);
        } else if (fieldDef instanceof Map) {
            return (Map<String, Object>) fieldDef;
        }
        return Map.of(); // invalid shape, will fail the type check above
    }
}