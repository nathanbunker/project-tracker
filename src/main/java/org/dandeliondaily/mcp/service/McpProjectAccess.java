package org.dandeliondaily.mcp.service;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.Workspace;

/** MCP writes stay within client scope; discovery additionally permits the owner's active patch memberships. */
public final class McpProjectAccess {
    private McpProjectAccess() { }

    public static List<Integer> readableWorkspaces(Session session, int workspaceId, WebUser user) {
        List<Integer> ids = new ArrayList<Integer>();
        ids.add(workspaceId);
        for (Workspace workspace : WorkspaceRegistry.getPatchWorkspacesForWebUser(session, user.getWebUserId())) {
            if (!ids.contains(workspace.getWorkspaceId())) {
                ids.add(workspace.getWorkspaceId());
            }
        }
        return ids;
    }

    public static Project requireReadable(Session session, int workspaceId, WebUser user, int projectId) {
        Project project = (Project) session.get(Project.class, projectId);
        if (project == null || project.getWorkspaceId() == null
                || !readableWorkspaces(session, workspaceId, user).contains(project.getWorkspaceId())) {
            throw new McpToolException("not_found", "Project not found.");
        }
        return project;
    }

    public static void requirePrivate(Session session, int workspaceId, WebUser user) {
        Workspace workspace = (Workspace) session.get(Workspace.class, workspaceId);
        if (workspace == null || !Workspace.TYPE_PRIVATE.equals(workspace.getWorkspaceType())
                || !Workspace.STATUS_ACTIVE.equals(workspace.getWorkspaceStatus())
                || !WorkspaceRegistry.hasActiveMembership(session, workspaceId, user.getWebUserId())) {
            throw new McpToolException("forbidden", "Project creation and moves require an active private workspace membership.");
        }
    }
}
