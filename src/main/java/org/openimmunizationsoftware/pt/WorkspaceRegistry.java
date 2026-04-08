package org.openimmunizationsoftware.pt;

import org.hibernate.Query;
import org.hibernate.Session;

public final class WorkspaceRegistry {

    public static final int NATHAN_WEB_USER_ID = 33;
    public static final int ABBIE_WEB_USER_ID = 45;

    public static final int NATHAN_WORKSPACE_ID = 1;
    public static final int ABBIE_WORKSPACE_ID = 2;

    private WorkspaceRegistry() {
    }

    public static Integer getWorkspaceIdForWebUserId(int webUserId) {
        if (webUserId == NATHAN_WEB_USER_ID) {
            return NATHAN_WORKSPACE_ID;
        }
        if (webUserId == ABBIE_WEB_USER_ID) {
            return ABBIE_WORKSPACE_ID;
        }
        return null;
    }

    public static Integer getWorkspaceIdForWebUserId(Session session, int webUserId) {
        Integer cached = getWorkspaceIdForWebUserId(webUserId);
        if (cached != null) {
            return cached;
        }
        Query query = session.createQuery(
                "select wm.workspaceId from WorkspaceMember wm where wm.webUserId = :webUserId");
        query.setInteger("webUserId", webUserId);
        query.setMaxResults(1);
        return (Integer) query.uniqueResult();
    }
}