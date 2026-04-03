package org.dandeliondaily.projecthealth.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectHealthPageModel {

    private int selectedProjectId;
    private String selectedProjectName = "";
    private boolean selectedProjectAvailable;

    private List<ProjectListItemModel> workProjects = new ArrayList<ProjectListItemModel>();
    private List<ProjectListItemModel> personalProjects = new ArrayList<ProjectListItemModel>();
    private ProjectReportModel report = new ProjectReportModel();
    private List<ProjectHealthIssueModel> issues = new ArrayList<ProjectHealthIssueModel>();

    public int getSelectedProjectId() {
        return selectedProjectId;
    }

    public void setSelectedProjectId(int selectedProjectId) {
        this.selectedProjectId = selectedProjectId;
    }

    public String getSelectedProjectName() {
        return selectedProjectName;
    }

    public void setSelectedProjectName(String selectedProjectName) {
        this.selectedProjectName = selectedProjectName;
    }

    public boolean isSelectedProjectAvailable() {
        return selectedProjectAvailable;
    }

    public void setSelectedProjectAvailable(boolean selectedProjectAvailable) {
        this.selectedProjectAvailable = selectedProjectAvailable;
    }

    public List<ProjectListItemModel> getWorkProjects() {
        return workProjects;
    }

    public void setWorkProjects(List<ProjectListItemModel> workProjects) {
        this.workProjects = workProjects;
    }

    public List<ProjectListItemModel> getPersonalProjects() {
        return personalProjects;
    }

    public void setPersonalProjects(List<ProjectListItemModel> personalProjects) {
        this.personalProjects = personalProjects;
    }

    public ProjectReportModel getReport() {
        return report;
    }

    public void setReport(ProjectReportModel report) {
        this.report = report;
    }

    public List<ProjectHealthIssueModel> getIssues() {
        return issues;
    }

    public void setIssues(List<ProjectHealthIssueModel> issues) {
        this.issues = issues;
    }
}
