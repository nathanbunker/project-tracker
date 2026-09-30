package org.openimmunizationsoftware.pt.api.v1.mcp;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/** Small helpers for reading MCP tool call arguments out of a Jackson JsonNode. */
public final class McpArgs {

    private McpArgs() {
    }

    public static String requireString(JsonNode arguments, String field) {
        String value = optString(arguments, field, null);
        if (value == null || value.trim().length() == 0) {
            throw new McpToolException("invalid_arguments", "\"" + field + "\" is required.");
        }
        return value;
    }

    public static String optString(JsonNode arguments, String field, String defaultValue) {
        if (arguments == null || !arguments.hasNonNull(field)) {
            return defaultValue;
        }
        return arguments.get(field).asText();
    }

    public static int requireInt(JsonNode arguments, String field) {
        if (arguments == null || !arguments.hasNonNull(field)) {
            throw new McpToolException("invalid_arguments", "\"" + field + "\" is required.");
        }
        return arguments.get(field).asInt();
    }

    public static Integer optInt(JsonNode arguments, String field, Integer defaultValue) {
        if (arguments == null || !arguments.hasNonNull(field)) {
            return defaultValue;
        }
        return Integer.valueOf(arguments.get(field).asInt());
    }

    public static List<String> optStringList(JsonNode arguments, String field) {
        List<String> result = new ArrayList<String>();
        if (arguments == null || !arguments.hasNonNull(field)) {
            return result;
        }
        JsonNode array = arguments.get(field);
        if (array.isArray()) {
            for (JsonNode item : array) {
                if (item != null && !item.isNull()) {
                    result.add(item.asText());
                }
            }
        }
        return result;
    }

    public static List<Integer> optIntList(JsonNode arguments, String field) {
        List<Integer> result = new ArrayList<Integer>();
        if (arguments == null || !arguments.hasNonNull(field)) {
            return result;
        }
        JsonNode array = arguments.get(field);
        if (array.isArray()) {
            for (JsonNode item : array) {
                if (item != null && !item.isNull()) {
                    result.add(Integer.valueOf(item.asInt()));
                }
            }
        }
        return result;
    }

    public static List<JsonNode> optNodeList(JsonNode arguments, String field) {
        List<JsonNode> result = new ArrayList<JsonNode>();
        if (arguments == null || !arguments.hasNonNull(field)) {
            return result;
        }
        JsonNode array = arguments.get(field);
        if (array.isArray()) {
            for (JsonNode item : array) {
                result.add(item);
            }
        }
        return result;
    }
}
