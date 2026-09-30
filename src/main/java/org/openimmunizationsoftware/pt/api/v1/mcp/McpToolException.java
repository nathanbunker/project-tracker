package org.openimmunizationsoftware.pt.api.v1.mcp;

/**
 * A tool-level failure (bad arguments, not found, business-rule violation)
 * that should be reported back to the MCP client as a failed tool call
 * rather than a transport-level JSON-RPC error.
 */
public class McpToolException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String code;

    public McpToolException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
