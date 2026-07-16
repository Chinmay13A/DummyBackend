package com.dummybackend.generatorservice.service;

import com.dummybackend.generatorservice.exception.SchemaValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SchemaValidator {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("string", "integer", "float", "boolean", "date", "enum", "object", "array");

    private static final int MAX_FIELDS = 30;
    private static final int MAX_DEPTH = 3;

    public void validate(Map<String, Object> schema) {
        List<String> errors = new ArrayList<>();
        validateFields(schema, "", 0, errors);
        if (!errors.isEmpty()) {
            throw new SchemaValidationException(errors);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateFields(Map<String, Object> fields, String pathPrefix, int depth, List<String> errors) {
        if (depth > MAX_DEPTH) {
            String location = pathPrefix.isEmpty() ? "schema" : "field \"" + pathPrefix + "\"";
            errors.add(location + " exceeds max nesting depth of " + MAX_DEPTH);
            return;
        }

        if (fields.size() > MAX_FIELDS) {
            String location = pathPrefix.isEmpty() ? "schema" : "field \"" + pathPrefix + "\"";
            errors.add(location + " cannot have more than " + MAX_FIELDS + " fields");
        }

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldPath = pathPrefix.isEmpty() ? entry.getKey() : pathPrefix + "." + entry.getKey();
            validateField(fieldPath, entry.getValue(), depth, errors);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateField(String fieldPath, Object fieldDef, int depth, List<String> errors) {
        Map<String, Object> normalized = normalize(fieldDef);
        Object typeObj = normalized.get("type");

        if (!(typeObj instanceof String type) || !ALLOWED_TYPES.contains(type)) {
            errors.add("field \"" + fieldPath + "\" has unsupported or missing type: " + typeObj);
            return;
        }

        if (type.equals("enum")) {
            Object values = normalized.get("values");
            if (!(values instanceof List<?> list) || list.isEmpty()) {
                errors.add("field \"" + fieldPath + "\" is type enum but missing non-empty \"values\" array");
            }
        }

        if (type.equals("integer") || type.equals("float")) {
            Object min = normalized.get("min");
            Object max = normalized.get("max");
            if (min instanceof Number minNum && max instanceof Number maxNum
                    && minNum.doubleValue() > maxNum.doubleValue()) {
                errors.add("field \"" + fieldPath + "\": min cannot be greater than max");
            }
        }

        if (type.equals("object")) {
            Object fieldsObj = normalized.get("fields");
            if (!(fieldsObj instanceof Map<?, ?> fieldsMap) || fieldsMap.isEmpty()) {
                errors.add("field \"" + fieldPath + "\" is type object but missing non-empty \"fields\" object");
                return;
            }
            validateFields((Map<String, Object>) fieldsMap, fieldPath, depth + 1, errors);
        }

        if (type.equals("array")) {
            Object items = normalized.get("items");
            if (items == null) {
                errors.add("field \"" + fieldPath + "\" is type array but missing \"items\" definition");
                return;
            }
            validateField(fieldPath + "[]", items, depth, errors);
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
