package org.dandeliondaily.dashboard.model;

import java.io.Serializable;
import java.util.Date;

public class ProjectDashboardChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String role;
    private String text;
    private Date createdDate = new Date();

    public ProjectDashboardChatMessage() {
    }

    public ProjectDashboardChatMessage(String role, String text) {
        this.role = role;
        this.text = text;
        this.createdDate = new Date();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isUser() {
        return "user".equalsIgnoreCase(role);
    }

    public boolean isAssistant() {
        return "assistant".equalsIgnoreCase(role);
    }
}
