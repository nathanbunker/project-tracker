package org.openimmunizationsoftware.pt.api.v1.resource;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.ApiRequestContext;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolRegistry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * A single MCP endpoint hosted inside Dandelion Daily itself, per
 * docs/Dandelion_Daily_AI_Integration_Assessment.md section 4-5 (Phase 0).
 * Hand-rolls the minimal MCP JSON-RPC surface (initialize, tools/list,
 * tools/call) rather than pulling in an SDK, since this app is on the
 * javax.* / Jersey 2 stack and the declared tool surface is intentionally
 * small (assessment section 6, decision 12).
 *
 * <p>Reuses the existing /api/* infrastructure as-is: HibernateSessionFilter
 * already wraps this whole request in one Session/Transaction, and
 * ApiKeyAuthFilter already authenticates the caller and binds
 * ApiRequestContext before this resource method runs.
 *
 * <p>Session-id handling is intentionally minimal: an Mcp-Session-Id is
 * issued on initialize for clients that expect one, but is not strictly
 * enforced on later calls, since every call is independently authenticated
 * by the X-Api-Key header regardless. This is a deliberate simplification
 * for a single trusted local client, not a full session lifecycle.
 */
@Path("/v1/mcp")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Hidden
public class McpResource {

    private static final String PROTOCOL_VERSION = "2025-06-18";
    private static final String SERVER_NAME = "dandelion-daily-mcp";
    private static final String SERVER_VERSION = "0.1.0";

    private final ObjectMapper mapper = new ObjectMapper();
    private final McpToolRegistry registry = new McpToolRegistry();

    @POST
    public Response handle(String rawBody) {
        JsonNode request;
        try {
            request = mapper.readTree(rawBody == null ? "" : rawBody);
        } catch (Exception parseError) {
            return Response.ok((Object) errorResponse(null, -32700, "Parse error")).build();
        }
        if (request == null || !request.isObject()) {
            return Response.ok((Object) errorResponse(null, -32700, "Parse error")).build();
        }

        JsonNode idNode = request.get("id");
        String method = request.hasNonNull("method") ? request.get("method").asText() : null;
        JsonNode params = request.get("params");
        boolean isNotification = idNode == null;

        if (method == null) {
            if (isNotification) {
                return Response.status(Response.Status.ACCEPTED).build();
            }
            return Response.ok((Object) errorResponse(idNode, -32600, "Invalid Request")).build();
        }

        if (isNotification) {
            // Notifications (e.g. notifications/initialized) get no JSON-RPC response.
            return Response.status(Response.Status.ACCEPTED).build();
        }

        try {
            if ("initialize".equals(method)) {
                return respondWithSession(successResponse(idNode, buildInitializeResult(params)));
            }
            if ("ping".equals(method)) {
                return Response.ok((Object) successResponse(idNode, mapper.createObjectNode())).build();
            }
            if ("tools/list".equals(method)) {
                return Response.ok((Object) successResponse(idNode, buildToolsListResult())).build();
            }
            if ("tools/call".equals(method)) {
                return Response.ok((Object) successResponse(idNode, callTool(params))).build();
            }
            return Response.ok((Object) errorResponse(idNode, -32601, "Method not found: " + method)).build();
        } catch (McpToolException unknownTool) {
            return Response.ok((Object) errorResponse(idNode, -32602, unknownTool.getMessage())).build();
        } catch (RuntimeException unexpected) {
            // Always log server-side: the message alone can be null (e.g.
            // UnsupportedOperationException), which otherwise leaves no trail to debug from.
            unexpected.printStackTrace(System.err);
            String detail = unexpected.getMessage() != null ? unexpected.getMessage()
                    : unexpected.getClass().getName();
            return Response.ok((Object) errorResponse(idNode, -32603, "Internal error: " + detail)).build();
        }
    }

    private Response respondWithSession(ObjectNode response) {
        return Response.ok((Object) response)
                .header("Mcp-Session-Id", UUID.randomUUID().toString())
                .build();
    }

    private ObjectNode buildInitializeResult(JsonNode params) {
        ObjectNode result = mapper.createObjectNode();
        String requestedVersion = params != null && params.hasNonNull("protocolVersion")
                ? params.get("protocolVersion").asText()
                : PROTOCOL_VERSION;
        result.put("protocolVersion", requestedVersion);
        ObjectNode capabilities = mapper.createObjectNode();
        capabilities.set("tools", mapper.createObjectNode());
        result.set("capabilities", capabilities);
        ObjectNode serverInfo = mapper.createObjectNode();
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);
        result.set("serverInfo", serverInfo);
        return result;
    }

    private ObjectNode buildToolsListResult() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode tools = mapper.createArrayNode();
        for (McpTool tool : registry.list()) {
            ObjectNode toolNode = mapper.createObjectNode();
            toolNode.put("name", tool.getName());
            toolNode.put("description", tool.getDescription());
            toolNode.set("inputSchema", mapper.valueToTree(tool.getInputSchema()));
            tools.add(toolNode);
        }
        result.set("tools", tools);
        return result;
    }

    private ObjectNode callTool(JsonNode params) {
        if (params == null || !params.hasNonNull("name")) {
            throw new McpToolException("invalid_params", "\"params.name\" is required.");
        }
        String toolName = params.get("name").asText();
        McpTool tool = registry.require(toolName);
        JsonNode arguments = params.get("arguments");

        ApiRequestContext.ApiClientInfo client = ApiRequestContext.getCurrentClient();
        if (client.getWorkspaceId() == null || client.getWorkspaceId().intValue() <= 0) {
            return toolErrorResult("forbidden", "This API client has no workspace scope.");
        }
        Session session = HibernateRequestContext.getCurrentSession();
        McpToolContext context = new McpToolContext(session, client.getWorkspaceId().intValue(),
                client.getUsername(), client.getAgentName());

        try {
            Object value = tool.call(arguments, context);
            return toolSuccessResult(value);
        } catch (McpToolException toolError) {
            rollbackIfActive(session);
            return toolErrorResult(toolError.getCode(), toolError.getMessage());
        } catch (RuntimeException unexpected) {
            rollbackIfActive(session);
            throw unexpected;
        }
    }

    /**
     * A tool that mutates data and then throws partway through must not leave
     * those mutations to be silently committed by HibernateSessionFilter's
     * end-of-request commit, since this resource always converts the
     * exception into a normal (non-exceptional) JSON-RPC response rather than
     * letting it propagate. Rolling back here, before that response is built,
     * is what makes every tool call's failure atomic. A no-op if nothing was
     * actually mutated.
     */
    private void rollbackIfActive(Session session) {
        try {
            if (session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } catch (RuntimeException rollbackFailure) {
            rollbackFailure.printStackTrace(System.err);
        }
    }

    private ObjectNode toolSuccessResult(Object value) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode content = mapper.createArrayNode();
        ObjectNode textBlock = mapper.createObjectNode();
        textBlock.put("type", "text");
        textBlock.put("text", toJsonText(value));
        content.add(textBlock);
        result.set("content", content);
        result.set("structuredContent", mapper.valueToTree(value));
        result.put("isError", false);
        return result;
    }

    private ObjectNode toolErrorResult(String code, String message) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode content = mapper.createArrayNode();
        ObjectNode textBlock = mapper.createObjectNode();
        textBlock.put("type", "text");
        textBlock.put("text", code + ": " + message);
        content.add(textBlock);
        result.set("content", content);
        result.put("isError", true);
        return result;
    }

    private String toJsonText(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private ObjectNode successResponse(JsonNode id, JsonNode result) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private ObjectNode errorResponse(JsonNode id, int code, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        return response;
    }
}
