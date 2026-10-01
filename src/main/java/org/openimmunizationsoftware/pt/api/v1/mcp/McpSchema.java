package org.openimmunizationsoftware.pt.api.v1.mcp;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tiny builder for the JSON-Schema-ish input schemas each tool declares. */
public final class McpSchema {

    private McpSchema() {
    }

    public static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required.length > 0) {
            schema.put("required", Arrays.asList(required));
        }
        schema.put("additionalProperties", Boolean.FALSE);
        return schema;
    }

    public static Map<String, Object> properties(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    public static Map<String, Object> string(String description) {
        return prop("string", description);
    }

    public static Map<String, Object> stringEnum(String description, String... values) {
        Map<String, Object> map = prop("string", description);
        map.put("enum", Arrays.asList(values));
        return map;
    }

    public static Map<String, Object> integer(String description) {
        return prop("integer", description);
    }

    public static Map<String, Object> bool(String description) {
        return prop("boolean", description);
    }

    public static Map<String, Object> array(String description, Map<String, Object> items) {
        Map<String, Object> map = prop("array", description);
        map.put("items", items);
        return map;
    }

    private static Map<String, Object> prop(String type, String description) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("type", type);
        if (description != null) {
            map.put("description", description);
        }
        return map;
    }
}
