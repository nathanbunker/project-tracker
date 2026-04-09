package org.openimmunizationsoftware.pt;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.Workspace;
import org.openimmunizationsoftware.pt.model.WorkspaceMember;

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
                "select wm.workspaceId from WorkspaceMember wm, Workspace w "
                        + "where wm.workspaceId = w.workspaceId "
                        + "and wm.webUserId = :webUserId "
                        + "and wm.membershipStatus = :membershipStatus "
                        + "and w.workspaceType = :workspaceType "
                        + "and w.workspaceStatus = :workspaceStatus "
                        + "order by wm.workspaceId");
        query.setInteger("webUserId", webUserId);
        query.setString("membershipStatus", WorkspaceMember.STATUS_ACTIVE);
        query.setString("workspaceType", Workspace.TYPE_PRIVATE);
        query.setString("workspaceStatus", Workspace.STATUS_ACTIVE);
        query.setMaxResults(1);
        return (Integer) query.uniqueResult();
    }

    public static boolean hasActiveMembership(Session session, int workspaceId, int webUserId) {
        Query query = session.createQuery("select count(*) from WorkspaceMember "
                + "where workspaceId = :workspaceId and webUserId = :webUserId "
                + "and membershipStatus = :membershipStatus");
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", webUserId);
        query.setString("membershipStatus", WorkspaceMember.STATUS_ACTIVE);
        Number count = (Number) query.uniqueResult();
        return count != null && count.intValue() > 0;
    }

    public static boolean canAdministerWorkspace(Session session, int workspaceId, int webUserId) {
        Query query = session.createQuery("select count(*) from WorkspaceMember "
                + "where workspaceId = :workspaceId and webUserId = :webUserId "
                + "and membershipStatus = :membershipStatus "
                + "and (memberRole = :ownerRole or memberRole = :adminRole)");
        query.setInteger("workspaceId", workspaceId);
        query.setInteger("webUserId", webUserId);
        query.setString("membershipStatus", WorkspaceMember.STATUS_ACTIVE);
        query.setString("ownerRole", WorkspaceMember.ROLE_OWNER);
        query.setString("adminRole", WorkspaceMember.ROLE_ADMIN);
        Number count = (Number) query.uniqueResult();
        return count != null && count.intValue() > 0;
    }

    @SuppressWarnings("unchecked")
    public static List<Workspace> getPatchWorkspacesForWebUser(Session session, int webUserId) {
        Query query = session.createQuery("from Workspace w "
                + "where w.workspaceType = :workspaceType "
                + "and w.workspaceStatus = :workspaceStatus "
                + "and exists (select wm.workspaceMemberId from WorkspaceMember wm "
                + "where wm.workspaceId = w.workspaceId "
                + "and wm.webUserId = :webUserId "
                + "and wm.membershipStatus = :membershipStatus) "
                + "order by w.workspaceName");
        query.setString("workspaceType", Workspace.TYPE_PATCH);
        query.setString("workspaceStatus", Workspace.STATUS_ACTIVE);
        query.setInteger("webUserId", webUserId);
        query.setString("membershipStatus", WorkspaceMember.STATUS_ACTIVE);
        return query.list();
    }
}