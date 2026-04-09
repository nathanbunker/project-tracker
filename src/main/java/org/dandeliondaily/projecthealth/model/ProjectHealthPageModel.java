package org.dandeliondaily.projecthealth.model;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.pt.model.Workspace;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectTag;

public class ProjectHealthPageModel {

    private int selectedProjectId;
    private String selectedProjectName = "";
    private boolean selectedProjectAvailable;
    private boolean healthCheckApplicable = true;

    private boolean selectedProjectIsPersonal;
    private boolean patchLinksVisible;
    private Integer selectedProjectLinkedPatchWorkspaceId;
    private Workspace selectedProjectLinkedPatchWorkspace;
    private boolean canChangePatchWorkspace = true;
    private List<ProjectPatchLinkDisplayModel> projectPatchLinks = new ArrayList<ProjectPatchLinkDisplayModel>();
    private List<Project> availablePatchProjects = new ArrayList<Project>();
    private List<ProjectTag> availablePatchTags = new ArrayList<ProjectTag>();

    private List<ProjectCadenceGroupModel> workProjectGroups = new ArrayList<ProjectCadenceGroupModel>();
    private List<ProjectCadenceGroupModel> personalProjectGroups = new ArrayList<ProjectCadenceGroupModel>();
    private List<ProjectListItemModel> pausedWorkProjects = new ArrayList<ProjectListItemModel>();
    private List<ProjectListItemModel> completedWorkProjects = new ArrayList<ProjectListItemModel>();
    private List<ProjectListItemModel> pausedPersonalProjects = new ArrayList<ProjectListItemModel>();
    private List<ProjectListItemModel> completedPersonalProjects = new ArrayList<ProjectListItemModel>();
    private ProjectReportModel report = new ProjectReportModel();
    private List<ProjectHealthIssueModel> issues = new ArrayList<ProjectHealthIssueModel>();
    private Integer contextWorkspaceId;
    private List<Workspace> accessiblePatchWorkspaces = new ArrayList<Workspace>();
    private boolean showContextSelector;

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

    public boolean isHealthCheckApplicable() {
        return healthCheckApplicable;
    }

    public void setHealthCheckApplicable(boolean healthCheckApplicable) {
        this.healthCheckApplicable = healthCheckApplicable;
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

    public List<ProjectListItemModel> getPausedWorkProjects() {
        return pausedWorkProjects;
    }

    public void setPausedWorkProjects(List<ProjectListItemModel> pausedWorkProjects) {
        this.pausedWorkProjects = pausedWorkProjects;
    }

    public List<ProjectListItemModel> getCompletedWorkProjects() {
        return completedWorkProjects;
    }

    public void setCompletedWorkProjects(List<ProjectListItemModel> completedWorkProjects) {
        this.completedWorkProjects = completedWorkProjects;
    }

    public List<ProjectListItemModel> getPausedPersonalProjects() {
        return pausedPersonalProjects;
    }

    public void setPausedPersonalProjects(List<ProjectListItemModel> pausedPersonalProjects) {
        this.pausedPersonalProjects = pausedPersonalProjects;
    }

    public List<ProjectListItemModel> getCompletedPersonalProjects() {
        return completedPersonalProjects;
    }

    public void setCompletedPersonalProjects(List<ProjectListItemModel> completedPersonalProjects) {
        this.completedPersonalProjects = completedPersonalProjects;
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

    public Integer getContextWorkspaceId() {
        return contextWorkspaceId;
    }

    public void setContextWorkspaceId(Integer contextWorkspaceId) {
        this.contextWorkspaceId = contextWorkspaceId;
    }

    public List<Workspace> getAccessiblePatchWorkspaces() {
        return accessiblePatchWorkspaces;
    }

    public void setAccessiblePatchWorkspaces(List<Workspace> accessiblePatchWorkspaces) {
        this.accessiblePatchWorkspaces = accessiblePatchWorkspaces;
    }

    public boolean isShowContextSelector() {
        return showContextSelector;
    }

    public void setShowContextSelector(boolean showContextSelector) {
        this.showContextSelector = showContextSelector;
    }

    public boolean isSelectedProjectIsPersonal() {
        return selectedProjectIsPersonal;
    }

    public void setSelectedProjectIsPersonal(boolean selectedProjectIsPersonal) {
        this.selectedProjectIsPersonal = selectedProjectIsPersonal;
    }

    public boolean isPatchLinksVisible() {
        return patchLinksVisible;
    }

    public void setPatchLinksVisible(boolean patchLinksVisible) {
        this.patchLinksVisible = patchLinksVisible;
    }

    public Integer getSelectedProjectLinkedPatchWorkspaceId() {
        return selectedProjectLinkedPatchWorkspaceId;
    }

    public void setSelectedProjectLinkedPatchWorkspaceId(Integer selectedProjectLinkedPatchWorkspaceId) {
        this.selectedProjectLinkedPatchWorkspaceId = selectedProjectLinkedPatchWorkspaceId;
    }

    public Workspace getSelectedProjectLinkedPatchWorkspace() {
        return selectedProjectLinkedPatchWorkspace;
    }

    public void setSelectedProjectLinkedPatchWorkspace(Workspace selectedProjectLinkedPatchWorkspace) {
        this.selectedProjectLinkedPatchWorkspace = selectedProjectLinkedPatchWorkspace;
    }

    public boolean isCanChangePatchWorkspace() {
        return canChangePatchWorkspace;
    }

    public void setCanChangePatchWorkspace(boolean canChangePatchWorkspace) {
        this.canChangePatchWorkspace = canChangePatchWorkspace;
    }

    public List<ProjectPatchLinkDisplayModel> getProjectPatchLinks() {
        return projectPatchLinks;
    }

    public void setProjectPatchLinks(List<ProjectPatchLinkDisplayModel> projectPatchLinks) {
        this.projectPatchLinks = projectPatchLinks;
    }

    public List<Project> getAvailablePatchProjects() {
        return availablePatchProjects;
    }

    public void setAvailablePatchProjects(List<Project> availablePatchProjects) {
        this.availablePatchProjects = availablePatchProjects;
    }

    public List<ProjectTag> getAvailablePatchTags() {
        return availablePatchTags;
    }

    public void setAvailablePatchTags(List<ProjectTag> availablePatchTags) {
        this.availablePatchTags = availablePatchTags;
    }
}
