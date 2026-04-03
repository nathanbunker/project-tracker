package org.dandeliondaily.projecthealth.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectHealthPageModel {

    private int selectedProjectId;
    private String selectedProjectName = "";
    private boolean selectedProjectAvailable;

    private List<ProjectCadenceGroupModel> workProjectGroups = new ArrayList<ProjectCadenceGroupModel>();
    private List<ProjectCadenceGroupModel> personalProjectGroups = new ArrayList<ProjectCadenceGroupModel>();
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

    public List<ProjectCadenceGroupModel> getWorkProjectGroups() {
        return workProjectGroups;
    }

    public void setWorkProjectGroups(List<ProjectCadenceGroupModel> workProjectGroups) {
        this.workProjectGroups = workProjectGroups;
    }

    public List<ProjectCadenceGroupModel> getPersonalProjectGroups() {
        return personalProjectGroups;
    }

    public void setPersonalProjectGroups(List<ProjectCadenceGroupModel> personalProjectGroups) {
        this.personalProjectGroups = personalProjectGroups;
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
