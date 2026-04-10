package org.dandeliondaily.dashboard.model;

import java.io.Serializable;

public class ProjectDashboardSuggestedIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String issueText = "";
    private String issueType = "";
    private String rationale = "";

    public String getIssueText() {
        return issueText;
    }

    public void setIssueText(String issueText) {
        this.issueText = issueText;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
