package org.dandeliondaily.projecthealth.model;

public class ProjectHealthIssueModel {

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    private Severity severity = Severity.INFO;
    private String title = "";
    private String detail = "";

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
