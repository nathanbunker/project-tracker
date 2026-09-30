package org.dandeliondaily.mcp.service;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolException;
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
        return owner;
    }
}
