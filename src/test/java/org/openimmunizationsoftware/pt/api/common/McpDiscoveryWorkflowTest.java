package org.openimmunizationsoftware.pt.api.common;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.Collections;
import javax.ws.rs.core.Response;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.api.v1.resource.McpResource;
import org.openimmunizationsoftware.pt.model.WebUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class McpDiscoveryWorkflowTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        WebUser user = new WebUser();
        user.setWebUserId(1);
        user.setUsername("discovery-test");
        Query query = (Query) Proxy.newProxyInstance(Query.class.getClassLoader(), new Class<?>[] {Query.class},
                (proxy, method, args) -> {
                    if (method.getName().startsWith("set")) { return proxy; }
                    if ("uniqueResult".equals(method.getName())) { return user; }
                    if ("list".equals(method.getName())) { return Collections.emptyList(); }
                    throw new UnsupportedOperationException(method.getName());
                });
        Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class<?>[] {Session.class},
                (proxy, method, args) -> {
                    if ("createQuery".equals(method.getName())) { return query; }
                    throw new UnsupportedOperationException(method.getName());
                });
        HibernateRequestContext.set(session);
        ApiRequestContext.set(new ApiRequestContext.ApiClientInfo(1, user.getUsername(), 4, "test-agent"));
    }

    @After
    public void cleanup() {
        HibernateRequestContext.clear();
        ApiRequestContext.clear();
    }

    @Test
    public void discoveryToolsAreAdvertisedWithStrictSchemas() {
        JsonNode tools = request("tools/list", null).path("result").path("tools");
        boolean projects = false;
        boolean search = false;
        for (JsonNode tool : tools) {
            if ("list_projects".equals(tool.path("name").asText())) {
                projects = true;
                assertFalse(tool.path("inputSchema").path("additionalProperties").asBoolean());
            }
            if ("search_actions".equals(tool.path("name").asText())) {
                search = true;
                assertEquals(3, tool.path("inputSchema").path("required").size());
            }
        }
        assertTrue(projects && search);
    }

    @Test
    public void listAndSearchDispatchThroughJsonRpc() {
        ObjectNode params = mapper.createObjectNode().put("name", "list_projects");
        params.putObject("arguments").put("scope", "ALL");
        JsonNode result = request("tools/call", params).path("result");
        assertFalse(result.path("isError").asBoolean());
        assertTrue(result.path("structuredContent").path("projects").isArray());
        params.put("name", "search_actions");
        params.putObject("arguments").put("text", "meeting").put("startDate", "2026-10-01")
                .put("endDate", "2026-10-31");
        result = request("tools/call", params).path("result");
        assertFalse(result.path("isError").asBoolean());
        assertTrue(result.path("structuredContent").path("actions").isArray());
    }

    private JsonNode request(String method, JsonNode params) {
        ObjectNode request = mapper.createObjectNode().put("jsonrpc", "2.0").put("id", 1).put("method", method);
        if (params != null) { request.set("params", params); }
        Response response = new McpResource().handle(request.toString());
        assertEquals(200, response.getStatus());
        return mapper.valueToTree(response.getEntity());
    }
}
