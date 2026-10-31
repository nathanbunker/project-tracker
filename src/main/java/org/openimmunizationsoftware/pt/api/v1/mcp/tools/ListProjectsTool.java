package org.openimmunizationsoftware.pt.api.v1.mcp.tools;

import java.util.Map;
import org.dandeliondaily.mcp.service.McpDiscoveryService;
import org.dandeliondaily.mcp.service.McpWebUserSupport;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpSchema;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.McpToolContext;
import com.fasterxml.jackson.databind.JsonNode;

public class ListProjectsTool implements McpTool {
    public String getName() { return "list_projects"; }
    public String getDescription() {
        return "Discovers projects in the client's workspace and the authenticated owner's active shared (PATCH) "
                + "workspace memberships, including projects with no actions or tracked time. Read-only. "
                + "scope ALL/PRIVATE/SHARED defaults ALL. status OPEN means Active/Paused (excluding Complete/Closed); "
                + "use ALL or an exact status. nameContains is literal case-insensitive text; tag is an exact "
                + "case-insensitive label. Stable project-name/ID pagination: limit default100 max200, offset default0. "
                + "Returns external sync identity/ownership, workspace, ownerWebUserId, billing, lastModifiedAt and "
                + "lastActivityAt (latest project modification, action change or recorded work; null if unknown). "
                + "Optional workspaceId and externalSourceKey filters select synchronized copies without merging "
                + "them or expanding authorization; no source is implicitly canonical. Discovery of shared projects does not grant write access.";
    }
    public Map<String, Object> getInputSchema() {
        return McpSchema.object(McpSchema.properties(
                "nameContains", McpSchema.string("Literal substring, max100."),
                "scope", McpSchema.stringEnum(null, "ALL", "PRIVATE", "SHARED"),
                "status", McpSchema.stringEnum(null, "OPEN", "ALL", "Active", "Paused", "Complete", "Closed"),
                "billCode", McpSchema.string("Exact bill code."),
                "tag", McpSchema.string("Exact tag label."),
                "workspaceId", McpSchema.integer("Optional positive workspace ID; existing read authorization still applies."),
                "externalSourceKey", McpSchema.string("Optional exact external source key, max100."),
                "limit", McpSchema.integer("1-200, default100."),
                "offset", McpSchema.integer("Nonnegative, default0.")));
    }
    public Object call(JsonNode args, McpToolContext context) {
        return new McpDiscoveryService().listProjects(context.getSession(), context.getWorkspaceId(),
                McpWebUserSupport.requireWebUser(context.getSession(), context.getUsername()), args);
    }
}
