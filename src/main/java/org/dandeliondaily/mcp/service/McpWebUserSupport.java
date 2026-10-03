package org.dandeliondaily.mcp.service;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

public final class McpWebUserSupport {

    private McpWebUserSupport() {
    }

    public static WebUser requireWebUser(Session session, String username) {
        Query query = session.createQuery("from WebUser where username = :username");
        query.setString("username", username);
        WebUser owner = (WebUser) query.uniqueResult();
        if (owner == null) {
            throw new McpToolException("not_found", "No user found for this API client.");
        }
        hydrateProjectContact(session, owner);
        return owner;
    }

    /**
     * WebUser.projectContact isn't mapped; web logins fill it in (LoginServlet,
     * AppReq), but a user loaded by query has it null. Code shared with the UI calls
     * setContact(webUser.getProjectContact()), and on entities whose contact_id is
     * written from contactId that silently saved contact_id = 0. Fill it in here so
     * every MCP tool sees the same WebUser a web session does.
     */
    static void hydrateProjectContact(Session session, WebUser owner) {
        if (owner.getProjectContact() == null && owner.getContactId() > 0) {
            owner.setProjectContact(
                    (ProjectContact) session.get(ProjectContact.class, Integer.valueOf(owner.getContactId())));
        }
    }
}
