package org.openimmunizationsoftware.pt.api.common;

public final class ApiRequestContext {

    private static final ThreadLocal<ApiClientInfo> CURRENT = new ThreadLocal<ApiClientInfo>();

    private ApiRequestContext() {
    }

    static void set(ApiClientInfo clientInfo) {
        CURRENT.set(clientInfo);
    }

    public static ApiClientInfo getCurrentClient() {
        ApiClientInfo clientInfo = CURRENT.get();
        if (clientInfo == null) {
            throw new IllegalStateException("No API client bound to current thread.");
        }
        return clientInfo;
    }

    static void clear() {
        CURRENT.remove();
    }

    public static final class ApiClientInfo {

        private final int clientId;
        private final String username;
        private final String providerId;
        private final String agentName;

        public ApiClientInfo(int clientId, String username, String providerId, String agentName) {
            this.clientId = clientId;
            this.username = username;
            this.providerId = providerId;
            this.agentName = agentName;
        }

        public int getClientId() {
            return clientId;
        }

        public String getUsername() {
            return username;
        }

        public String getProviderId() {
            return providerId;
        }

        public String getAgentName() {
            return agentName;
        }
    }
}
