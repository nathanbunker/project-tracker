package org.openimmunizationsoftware.pt.api.v1.mcp;

import org.hibernate.Session;

/**
 * Per-call context resolved once from the authenticated WebApiClient
 * (see org.openimmunizationsoftware.pt.api.common.ApiKeyAuthFilter) and
 * handed to every tool invocation.
 */
public class McpToolContext {

    private final Session session;
    private final int workspaceId;
    private final String username;
    private final String agentName;

    public McpToolContext(Session session, int workspaceId, String username, String agentName) {
        this.session = session;
        this.workspaceId = workspaceId;
        this.username = username;
        this.agentName = agentName;
    }

    public Session getSession() {
        return session;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public String getUsername() {
        return username;
    }

    public String getAgentName() {
        return agentName;
    }
}
