package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class WebApiClient implements java.io.Serializable {

    private static final long serialVersionUID = 4102430100630391979L;

    private int clientId;
    private String apiKey;
    private WebUser webUser;
    private String providerId;
    private String agentName;
    private boolean enabled = true;
    private Date createDate;
    private Date lastUsedDate;

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getUsername() {
        return webUser != null ? webUser.getUsername() : null;
    }

    public void setUsername(String username) {
        // no-op: use setWebUser() instead
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(Date lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }
}
