package org.dandeliondaily.projecthealth.model;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.pt.model.ProjectFactDefinition;
import org.openimmunizationsoftware.pt.model.Workspace;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectTag;

public class ProjectHealthPageModel {

    public static class SharedProjectFactItemModel {
        private int projectFactDefinitionId;
        private String factLabel = "";
        private boolean checked;

        public int getProjectFactDefinitionId() {
            return projectFactDefinitionId;
        }

        public void setProjectFactDefinitionId(int projectFactDefinitionId) {
            this.projectFactDefinitionId = projectFactDefinitionId;
        }

        public String getFactLabel() {
            return factLabel;
        }

        public void setFactLabel(String factLabel) {
            this.factLabel = factLabel;
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    public static class SharedProjectFactGroupModel {
        private String factGroup = "";
        private int checkedCount;
        private int totalCount;
        private List<SharedProjectFactItemModel> items = new ArrayList<SharedProjectFactItemModel>();

        public String getFactGroup() {
            return factGroup;
        }

        public void setFactGroup(String factGroup) {
            this.factGroup = factGroup;
        }

        public int getCheckedCount() {
            return checkedCount;
        }

        public void setCheckedCount(int checkedCount) {
            this.checkedCount = checkedCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public List<SharedProjectFactItemModel> getItems() {
            return items;
        }

        public void setItems(List<SharedProjectFactItemModel> items) {
            this.items = items;
        }
    }

    public static class OpenActionActorOptionModel {
        private Integer contactId;
        private String label = "";

        public Integer getContactId() {
            return contactId;
        }

        public void setContactId(Integer contactId) {
            this.contactId = contactId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class OpenActionItemModel {
        private int actionNextId;
        private Integer actorContactId;
        private String actorDisplay = "";
        private String sentenceHtml = "";
        private String nextActionType = "";
        private String nextDescription = "";
        private String nextActionDateLabel = "";
        private String nextActionDateInput = "";
        private Integer nextTimeEstimate;
        private String nextTimeEstimateLabel = "";
        private String targetDateLabel = "";
        private String deadlineDateLabel = "";
        private int priorityLevel;
        private int completionOrder;
        private boolean linkedToSelectedPrivate;
        private Integer linkedPrivateActionNextId;

        public int getActionNextId() {
            return actionNextId;
        }

        public void setActionNextId(int actionNextId) {
            this.actionNextId = actionNextId;
        }

        public Integer getActorContactId() {
            return actorContactId;
        }

        public void setActorContactId(Integer actorContactId) {
            this.actorContactId = actorContactId;
        }

        public String getActorDisplay() {
            return actorDisplay;
        }

        public void setActorDisplay(String actorDisplay) {
            this.actorDisplay = actorDisplay;
        }

        public String getSentenceHtml() {
            return sentenceHtml;
        }

        public void setSentenceHtml(String sentenceHtml) {
            this.sentenceHtml = sentenceHtml;
        }

        public String getNextActionType() {
            return nextActionType;
        }

        public void setNextActionType(String nextActionType) {
            this.nextActionType = nextActionType;
        }

        public String getNextDescription() {
            return nextDescription;
        }

        public void setNextDescription(String nextDescription) {
            this.nextDescription = nextDescription;
        }

        public String getNextActionDateLabel() {
            return nextActionDateLabel;
        }

        public void setNextActionDateLabel(String nextActionDateLabel) {
            this.nextActionDateLabel = nextActionDateLabel;
        }

        public String getNextActionDateInput() {
            return nextActionDateInput;
        }

        public void setNextActionDateInput(String nextActionDateInput) {
            this.nextActionDateInput = nextActionDateInput;
        }

        public Integer getNextTimeEstimate() {
            return nextTimeEstimate;
        }

        public void setNextTimeEstimate(Integer nextTimeEstimate) {
            this.nextTimeEstimate = nextTimeEstimate;
        }

        public String getNextTimeEstimateLabel() {
            return nextTimeEstimateLabel;
        }

        public void setNextTimeEstimateLabel(String nextTimeEstimateLabel) {
            this.nextTimeEstimateLabel = nextTimeEstimateLabel;
        }

        public String getTargetDateLabel() {
            return targetDateLabel;
        }

        public void setTargetDateLabel(String targetDateLabel) {
            this.targetDateLabel = targetDateLabel;
        }

        public String getDeadlineDateLabel() {
            return deadlineDateLabel;
        }

        public void setDeadlineDateLabel(String deadlineDateLabel) {
            this.deadlineDateLabel = deadlineDateLabel;
        }

        public int getPriorityLevel() {
            return priorityLevel;
        }

        public void setPriorityLevel(int priorityLevel) {
            this.priorityLevel = priorityLevel;
        }

        public int getCompletionOrder() {
            return completionOrder;
        }

        public void setCompletionOrder(int completionOrder) {
            this.completionOrder = completionOrder;
        }

        public boolean isLinkedToSelectedPrivate() {
            return linkedToSelectedPrivate;
        }

        public void setLinkedToSelectedPrivate(boolean linkedToSelectedPrivate) {
            this.linkedToSelectedPrivate = linkedToSelectedPrivate;
        }

        public Integer getLinkedPrivateActionNextId() {
            return linkedPrivateActionNextId;
        }

        public void setLinkedPrivateActionNextId(Integer linkedPrivateActionNextId) {
            this.linkedPrivateActionNextId = linkedPrivateActionNextId;
        }
    }

    public static final String LEFT_PANEL_MODE_PRIVATE = "PRIVATE";
    public static final String LEFT_PANEL_MODE_PATCH_SUMMARY = "PATCH_SUMMARY";
    public static final String LEFT_PANEL_MODE_PATCH_TAG = "PATCH_TAG";
    public static final String PATCH_TAG_KEY_UNTAGGED = "UNTAGGED";

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
    private List<Project> linkedPrivateProjects = new ArrayList<Project>();
    private List<Project> candidatePrivateProjects = new ArrayList<Project>();
    private Integer selectedPrivateProjectId;
    private Project selectedPrivateProject;
    private Integer openActionEditActionNextId;
    private boolean openActionsNeedsPrivateTargetSelection;
    private List<OpenActionActorOptionModel> openActionActorOptions = new ArrayList<OpenActionActorOptionModel>();
    private List<OpenActionItemModel> openScheduledActions = new ArrayList<OpenActionItemModel>();
    private List<OpenActionItemModel> openUnscheduledActions = new ArrayList<OpenActionItemModel>();
    private List<SharedProjectFactGroupModel> sharedProjectFactGroups = new ArrayList<SharedProjectFactGroupModel>();

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
    private String leftPanelMode = LEFT_PANEL_MODE_PRIVATE;
    private String selectedPatchTagKey;
    private String selectedPatchTagLabel;
    private List<ProjectTagSummaryRowModel> patchTagSummaryRows = new ArrayList<ProjectTagSummaryRowModel>();
    private List<ProjectCadenceGroupModel> patchTagProjectGroups = new ArrayList<ProjectCadenceGroupModel>();

    private boolean factsMode;
    private String factsMessage = "";
    private boolean factsMessageError;
    private Integer selectedFactDefinitionId;
    private String selectedFactGroup;
    private List<ProjectFactDefinition> factDefinitions = new ArrayList<ProjectFactDefinition>();
    private ProjectFactDefinition selectedFactDefinition;

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

    public List<Project> getLinkedPrivateProjects() {
        return linkedPrivateProjects;
    }

    public void setLinkedPrivateProjects(List<Project> linkedPrivateProjects) {
        this.linkedPrivateProjects = linkedPrivateProjects;
    }

    public List<Project> getCandidatePrivateProjects() {
        return candidatePrivateProjects;
    }

    public void setCandidatePrivateProjects(List<Project> candidatePrivateProjects) {
        this.candidatePrivateProjects = candidatePrivateProjects;
    }

    public Integer getSelectedPrivateProjectId() {
        return selectedPrivateProjectId;
    }

    public void setSelectedPrivateProjectId(Integer selectedPrivateProjectId) {
        this.selectedPrivateProjectId = selectedPrivateProjectId;
    }

    public Project getSelectedPrivateProject() {
        return selectedPrivateProject;
    }

    public void setSelectedPrivateProject(Project selectedPrivateProject) {
        this.selectedPrivateProject = selectedPrivateProject;
    }

    public Integer getOpenActionEditActionNextId() {
        return openActionEditActionNextId;
    }

    public void setOpenActionEditActionNextId(Integer openActionEditActionNextId) {
        this.openActionEditActionNextId = openActionEditActionNextId;
    }

    public boolean isOpenActionsNeedsPrivateTargetSelection() {
        return openActionsNeedsPrivateTargetSelection;
    }

    public void setOpenActionsNeedsPrivateTargetSelection(boolean openActionsNeedsPrivateTargetSelection) {
        this.openActionsNeedsPrivateTargetSelection = openActionsNeedsPrivateTargetSelection;
    }

    public List<OpenActionActorOptionModel> getOpenActionActorOptions() {
        return openActionActorOptions;
    }

    public void setOpenActionActorOptions(List<OpenActionActorOptionModel> openActionActorOptions) {
        this.openActionActorOptions = openActionActorOptions;
    }

    public List<OpenActionItemModel> getOpenScheduledActions() {
        return openScheduledActions;
    }

    public void setOpenScheduledActions(List<OpenActionItemModel> openScheduledActions) {
        this.openScheduledActions = openScheduledActions;
    }

    public List<OpenActionItemModel> getOpenUnscheduledActions() {
        return openUnscheduledActions;
    }

    public void setOpenUnscheduledActions(List<OpenActionItemModel> openUnscheduledActions) {
        this.openUnscheduledActions = openUnscheduledActions;
    }

    public List<SharedProjectFactGroupModel> getSharedProjectFactGroups() {
        return sharedProjectFactGroups;
    }

    public void setSharedProjectFactGroups(List<SharedProjectFactGroupModel> sharedProjectFactGroups) {
        this.sharedProjectFactGroups = sharedProjectFactGroups;
    }

    public String getLeftPanelMode() {
        return leftPanelMode;
    }

    public void setLeftPanelMode(String leftPanelMode) {
        this.leftPanelMode = leftPanelMode;
    }

    public String getSelectedPatchTagKey() {
        return selectedPatchTagKey;
    }

    public void setSelectedPatchTagKey(String selectedPatchTagKey) {
        this.selectedPatchTagKey = selectedPatchTagKey;
    }

    public String getSelectedPatchTagLabel() {
        return selectedPatchTagLabel;
    }

    public void setSelectedPatchTagLabel(String selectedPatchTagLabel) {
        this.selectedPatchTagLabel = selectedPatchTagLabel;
    }

    public List<ProjectTagSummaryRowModel> getPatchTagSummaryRows() {
        return patchTagSummaryRows;
    }

    public void setPatchTagSummaryRows(List<ProjectTagSummaryRowModel> patchTagSummaryRows) {
        this.patchTagSummaryRows = patchTagSummaryRows;
    }

    public List<ProjectCadenceGroupModel> getPatchTagProjectGroups() {
        return patchTagProjectGroups;
    }

    public void setPatchTagProjectGroups(List<ProjectCadenceGroupModel> patchTagProjectGroups) {
        this.patchTagProjectGroups = patchTagProjectGroups;
    }

    public boolean isPatchContext() {
        return contextWorkspaceId != null;
    }

    public boolean isPatchSummaryMode() {
        return LEFT_PANEL_MODE_PATCH_SUMMARY.equals(leftPanelMode);
    }

    public boolean isPatchTagMode() {
        return LEFT_PANEL_MODE_PATCH_TAG.equals(leftPanelMode);
    }

    public boolean isFactsMode() {
        return factsMode;
    }

    public void setFactsMode(boolean factsMode) {
        this.factsMode = factsMode;
    }

    public String getFactsMessage() {
        return factsMessage;
    }

    public void setFactsMessage(String factsMessage) {
        this.factsMessage = factsMessage;
    }

    public boolean isFactsMessageError() {
        return factsMessageError;
    }

    public void setFactsMessageError(boolean factsMessageError) {
        this.factsMessageError = factsMessageError;
    }

    public Integer getSelectedFactDefinitionId() {
        return selectedFactDefinitionId;
    }

    public void setSelectedFactDefinitionId(Integer selectedFactDefinitionId) {
        this.selectedFactDefinitionId = selectedFactDefinitionId;
    }

    public String getSelectedFactGroup() {
        return selectedFactGroup;
    }

    public void setSelectedFactGroup(String selectedFactGroup) {
        this.selectedFactGroup = selectedFactGroup;
    }

    public List<ProjectFactDefinition> getFactDefinitions() {
        return factDefinitions;
    }

    public void setFactDefinitions(List<ProjectFactDefinition> factDefinitions) {
        this.factDefinitions = factDefinitions;
    }

    public ProjectFactDefinition getSelectedFactDefinition() {
        return selectedFactDefinition;
    }

    public void setSelectedFactDefinition(ProjectFactDefinition selectedFactDefinition) {
        this.selectedFactDefinition = selectedFactDefinition;
    }
}
