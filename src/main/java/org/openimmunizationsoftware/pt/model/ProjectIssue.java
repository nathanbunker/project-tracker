package org.openimmunizationsoftware.pt.model;

import java.io.Serializable;
import java.util.Date;

public class ProjectIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    private int projectIssueId;
    private Project project;
    private String issueText;
    private String issueTypeString;
    private String issueStatusString;
    private Date createdDate;
    private Date updatedDate;
    private Date resolvedDate;

    public int getProjectIssueId() {
        return projectIssueId;
    }

    public void setProjectIssueId(int projectIssueId) {
        this.projectIssueId = projectIssueId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getIssueText() {
        return issueText;
    }

    public void setIssueText(String issueText) {
        this.issueText = issueText;
    }

    public String getIssueTypeString() {
        return issueTypeString;
    }

    public void setIssueTypeString(String issueTypeString) {
        this.issueTypeString = issueTypeString;
    }

    public ProjectIssueType getIssueType() {
        return ProjectIssueType.fromString(issueTypeString);
    }

    public void setIssueType(ProjectIssueType issueType) {
        this.issueTypeString = issueType != null ? issueType.name() : ProjectIssueType.UNKNOWN.name();
    }

    public String getIssueStatusString() {
        return issueStatusString;
    }

    public void setIssueStatusString(String issueStatusString) {
        this.issueStatusString = issueStatusString;
    }

    public ProjectIssueStatus getIssueStatus() {
        return ProjectIssueStatus.fromString(issueStatusString);
    }

    public void setIssueStatus(ProjectIssueStatus issueStatus) {
        this.issueStatusString = issueStatus != null ? issueStatus.name() : ProjectIssueStatus.OPEN.name();
    }

    public boolean isOpen() {
        return ProjectIssueStatus.OPEN == getIssueStatus();
    }

    public boolean isResolved() {
        return ProjectIssueStatus.RESOLVED == getIssueStatus();
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Date getResolvedDate() {
        return resolvedDate;
    }

    public void setResolvedDate(Date resolvedDate) {
        this.resolvedDate = resolvedDate;
    }
}
