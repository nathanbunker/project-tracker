package org.openimmunizationsoftware.pt.api.v1.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.pt.api.v1.mcp.tools.GetDayAvailabilityTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.tools.GetOutlookTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.tools.GetPlanningContextTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.tools.GetProjectContextTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.tools.GetTimeAllocationContextTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.tools.ListOutlooksTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.tools.SetDayAvailabilityTool;
import org.openimmunizationsoftware.pt.api.v1.mcp.tools.SetOutlookTool;

/** The small, task-oriented tool surface for Phase 0 (see assessment section 4-5). */
public class McpToolRegistry {

    private final Map<String, McpTool> toolsByName = new LinkedHashMap<String, McpTool>();

    public McpToolRegistry() {
        register(new GetProjectContextTool());
        register(new GetPlanningContextTool());
        register(new GetOutlookTool());
        register(new ListOutlooksTool());
        register(new SetOutlookTool());
        register(new GetTimeAllocationContextTool());
        register(new GetDayAvailabilityTool());
        register(new SetDayAvailabilityTool());
    }

    private void register(McpTool tool) {
        toolsByName.put(tool.getName(), tool);
    }

    public List<McpTool> list() {
        return new ArrayList<McpTool>(toolsByName.values());
    }

    public McpTool require(String name) {
        McpTool tool = toolsByName.get(name);
        if (tool == null) {
            throw new McpToolException("unknown_tool", "No tool named \"" + name + "\".");
        }
        return tool;
    }
}
