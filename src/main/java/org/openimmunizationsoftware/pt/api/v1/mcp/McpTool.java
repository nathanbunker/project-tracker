package org.openimmunizationsoftware.pt.api.v1.mcp;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A single MCP tool: task-oriented, not raw CRUD, per
 * docs/Dandelion_Daily_AI_Integration_Assessment.md section 4.
 */
public interface McpTool {

    String getName();

    String getDescription();

    /** A JSON Schema object (as a plain Map, serialized by the resource layer). */
    Map<String, Object> getInputSchema();

    /** Returns a plain value (Map/List/String/etc.) to be serialized as the tool result. */
    Object call(JsonNode arguments, McpToolContext context);
}
