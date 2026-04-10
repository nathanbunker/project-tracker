package org.dandeliondaily.dashboard.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DashboardNowColumnModel {

    private CurrentAction currentAction = new CurrentAction();
    private CurrentProject currentProject = new CurrentProject();
    private List<ScheduledActionItem> scheduledActions = new ArrayList<>();
    private List<UnscheduledActionItem> ideaActions = new ArrayList<>();
    private List<UnscheduledActionItem> unscheduledActions = new ArrayList<>();
    private List<TemplatedActionItem> templatedActions = new ArrayList<>();
    private List<RecentCompletedItem> recentCompleted = new ArrayList<>();
    private List<OpenIssueItem> openIssues = new ArrayList<>();
    private List<TakenActionItem> takenToday = new ArrayList<>();
    private List<TakenActionItem> takenActions = new ArrayList<>();
    private ProjectHealthSection projectHealth = new ProjectHealthSection();
    private List<ProposalActionItem> proposalActions = new ArrayList<ProposalActionItem>();
    private List<NarrativeItem> narrativeItems = new ArrayList<NarrativeItem>();
    private int narrativeTotalCount;

    public CurrentAction getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(CurrentAction currentAction) {
        this.currentAction = currentAction;
    }

    public CurrentProject getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(CurrentProject currentProject) {
        this.currentProject = currentProject;
    }

    public List<ScheduledActionItem> getScheduledActions() {
        return scheduledActions;
    }

    public void setScheduledActions(List<ScheduledActionItem> scheduledActions) {
        this.scheduledActions = scheduledActions;
    }

    public List<UnscheduledActionItem> getUnscheduledActions() {
        return unscheduledActions;
    }

    public List<UnscheduledActionItem> getIdeaActions() {
        return ideaActions;
    }

    public void setIdeaActions(List<UnscheduledActionItem> ideaActions) {
        this.ideaActions = ideaActions;
    }

    public void setUnscheduledActions(List<UnscheduledActionItem> unscheduledActions) {
        this.unscheduledActions = unscheduledActions;
    }

    public List<TemplatedActionItem> getTemplatedActions() {
        return templatedActions;
    }

    public void setTemplatedActions(List<TemplatedActionItem> templatedActions) {
        this.templatedActions = templatedActions;
    }

    public List<RecentCompletedItem> getRecentCompleted() {
        return recentCompleted;
    }

    public void setRecentCompleted(List<RecentCompletedItem> recentCompleted) {
        this.recentCompleted = recentCompleted;
    }

    public List<OpenIssueItem> getOpenIssues() {
        return openIssues;
    }

    public void setOpenIssues(List<OpenIssueItem> openIssues) {
        this.openIssues = openIssues;
    }

    public List<TakenActionItem> getTakenToday() {
        return takenToday;
    }

    public void setTakenToday(List<TakenActionItem> takenToday) {
        this.takenToday = takenToday;
    }

    public List<TakenActionItem> getTakenActions() {
        return takenActions;
    }

    public void setTakenActions(List<TakenActionItem> takenActions) {
        this.takenActions = takenActions;
    }

    public ProjectHealthSection getProjectHealth() {
        return projectHealth;
    }

    public void setProjectHealth(ProjectHealthSection projectHealth) {
        this.projectHealth = projectHealth;
    }

    public List<ProposalActionItem> getProposalActions() {
        return proposalActions;
    }

    public void setProposalActions(List<ProposalActionItem> proposalActions) {
        this.proposalActions = proposalActions;
    }

    public List<NarrativeItem> getNarrativeItems() {
        return narrativeItems;
    }

    public void setNarrativeItems(List<NarrativeItem> narrativeItems) {
        this.narrativeItems = narrativeItems;
    }

    public int getNarrativeTotalCount() {
        return narrativeTotalCount;
    }

    public void setNarrativeTotalCount(int narrativeTotalCount) {
        this.narrativeTotalCount = narrativeTotalCount;
    }

    public static class CurrentAction {
        private boolean available;
        private boolean trackable;
        private int actionNextId;
        private String title = "";
        private String summary = "";
        private String descriptionHtml = "";
        private String estimateDisplay = "";
        private String timeSpentTodayDisplay = "";
        private String statusLabel = "";
        private String actionTypeLabel = "";
        private List<String> notes = new ArrayList<String>();
        private String linkUrl = "";
        private String deadlineDisplay = "";
        private String fallbackMessage = "No current action selected";

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public boolean isTrackable() {
            return trackable;
        }

        public void setTrackable(boolean trackable) {
            this.trackable = trackable;
        }

        public int getActionNextId() {
            return actionNextId;
        }

        public void setActionNextId(int actionNextId) {
            this.actionNextId = actionNextId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getDescriptionHtml() {
            return descriptionHtml;
        }

        public void setDescriptionHtml(String descriptionHtml) {
            this.descriptionHtml = descriptionHtml;
        }

        public String getEstimateDisplay() {
            return estimateDisplay;
        }

        public void setEstimateDisplay(String estimateDisplay) {
            this.estimateDisplay = estimateDisplay;
        }

        public String getTimeSpentTodayDisplay() {
            return timeSpentTodayDisplay;
        }

        public void setTimeSpentTodayDisplay(String timeSpentTodayDisplay) {
            this.timeSpentTodayDisplay = timeSpentTodayDisplay;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel;
        }

        public String getActionTypeLabel() {
            return actionTypeLabel;
        }

        public void setActionTypeLabel(String actionTypeLabel) {
            this.actionTypeLabel = actionTypeLabel;
        }

        public List<String> getNotes() {
            return notes;
        }

        public void setNotes(List<String> notes) {
            this.notes = notes;
        }

        public String getLinkUrl() {
            return linkUrl;
        }

        public void setLinkUrl(String linkUrl) {
            this.linkUrl = linkUrl;
        }

        public String getDeadlineDisplay() {
            return deadlineDisplay;
        }

        public void setDeadlineDisplay(String deadlineDisplay) {
            this.deadlineDisplay = deadlineDisplay;
        }

        public String getFallbackMessage() {
            return fallbackMessage;
        }

        public void setFallbackMessage(String fallbackMessage) {
            this.fallbackMessage = fallbackMessage;
        }
    }

    public static class CurrentProject {
        private boolean available;
        private int projectId;
        private String name = "";
        private String handle = "";
        private String description = "";
        private String fallbackMessage = "No current project selected";

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public int getProjectId() {
            return projectId;
        }

        public void setProjectId(int projectId) {
            this.projectId = projectId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHandle() {
            return handle;
        }

        public void setHandle(String handle) {
            this.handle = handle;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getFallbackMessage() {
            return fallbackMessage;
        }

        public void setFallbackMessage(String fallbackMessage) {
            this.fallbackMessage = fallbackMessage;
        }
    }

    public static class ScheduledActionItem {
        private String dateLabel = "";
        private String descriptionHtml = "";
        private boolean currentSelection;
        private int actionNextId;
        private boolean today;

        public String getDateLabel() {
            return dateLabel;
        }

        public void setDateLabel(String dateLabel) {
            this.dateLabel = dateLabel;
        }

        public String getDescriptionHtml() {
            return descriptionHtml;
        }

        public void setDescriptionHtml(String descriptionHtml) {
            this.descriptionHtml = descriptionHtml;
        }

        public boolean isCurrentSelection() {
            return currentSelection;
        }

        public void setCurrentSelection(boolean currentSelection) {
            this.currentSelection = currentSelection;
        }

        public int getActionNextId() {
            return actionNextId;
        }

        public void setActionNextId(int actionNextId) {
            this.actionNextId = actionNextId;
        }

        public boolean isToday() {
            return today;
        }

        public void setToday(boolean today) {
            this.today = today;
        }
    }

    public static class UnscheduledActionItem {
        private String descriptionHtml = "";
        private boolean currentSelection;
        private int actionNextId;

        public String getDescriptionHtml() {
            return descriptionHtml;
        }

        public void setDescriptionHtml(String descriptionHtml) {
            this.descriptionHtml = descriptionHtml;
        }

        public boolean isCurrentSelection() {
            return currentSelection;
        }

        public void setCurrentSelection(boolean currentSelection) {
            this.currentSelection = currentSelection;
        }

        public int getActionNextId() {
            return actionNextId;
        }

        public void setActionNextId(int actionNextId) {
            this.actionNextId = actionNextId;
        }
    }

    public static class TemplatedActionItem {
        private String descriptionHtml = "";
        private int actionNextId;

        public String getDescriptionHtml() {
            return descriptionHtml;
        }

        public void setDescriptionHtml(String descriptionHtml) {
            this.descriptionHtml = descriptionHtml;
        }

        public int getActionNextId() {
            return actionNextId;
        }

        public void setActionNextId(int actionNextId) {
            this.actionNextId = actionNextId;
        }
    }

    public static class RecentCompletedItem {
        private int actionNextId;
        private String dateLabel = "";
        private String whatHappened = "";

        public int getActionNextId() {
            return actionNextId;
        }

        public void setActionNextId(int actionNextId) {
            this.actionNextId = actionNextId;
        }

        public String getDateLabel() {
            return dateLabel;
        }

        public void setDateLabel(String dateLabel) {
            this.dateLabel = dateLabel;
        }

        public String getWhatHappened() {
            return whatHappened;
        }

        public void setWhatHappened(String whatHappened) {
            this.whatHappened = whatHappened;
        }
    }

    public static class OpenIssueItem {
        private int projectIssueId;
        private String issueText = "";
        private String issueTypeEmoji = "";
        private String issueTypeValue = "";
        private String createdDisplay = "";
        private Date createdDate;

        public int getProjectIssueId() {
            return projectIssueId;
        }

        public void setProjectIssueId(int projectIssueId) {
            this.projectIssueId = projectIssueId;
        }

        public String getIssueText() {
            return issueText;
        }

        public void setIssueText(String issueText) {
            this.issueText = issueText;
        }

        public String getIssueTypeEmoji() {
            return issueTypeEmoji;
        }

        public void setIssueTypeEmoji(String issueTypeEmoji) {
            this.issueTypeEmoji = issueTypeEmoji;
        }

        public String getIssueTypeValue() {
            return issueTypeValue;
        }

        public void setIssueTypeValue(String issueTypeValue) {
            this.issueTypeValue = issueTypeValue;
        }

        public String getCreatedDisplay() {
            return createdDisplay;
        }

        public void setCreatedDisplay(String createdDisplay) {
            this.createdDisplay = createdDisplay;
        }

        public Date getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }
    }

    public static class TakenActionItem {
        private int actionTakenId;
        private String dateLabel = "";
        private String description = "";
        private String whoLabel = "";

        public int getActionTakenId() {
            return actionTakenId;
        }

        public void setActionTakenId(int actionTakenId) {
            this.actionTakenId = actionTakenId;
        }

        public String getDateLabel() {
            return dateLabel;
        }

        public void setDateLabel(String dateLabel) {
            this.dateLabel = dateLabel;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getWhoLabel() {
            return whoLabel;
        }

        public void setWhoLabel(String whoLabel) {
            this.whoLabel = whoLabel;
        }
    }

    public static class ProjectHealthSection {
        private String projectName = "";
        private String projectHandle = "";
        private String projectStatus = "";
        private String description = "";
        private String outcome = "";
        private List<String> successCriteriaItems = new ArrayList<String>();
        private List<ProjectHealthIssueItem> issues = new ArrayList<ProjectHealthIssueItem>();
        private int actionableIssueCount;

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getProjectHandle() {
            return projectHandle;
        }

        public void setProjectHandle(String projectHandle) {
            this.projectHandle = projectHandle;
        }

        public String getProjectStatus() {
            return projectStatus;
        }

        public void setProjectStatus(String projectStatus) {
            this.projectStatus = projectStatus;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getOutcome() {
            return outcome;
        }

        public void setOutcome(String outcome) {
            this.outcome = outcome;
        }

        public List<String> getSuccessCriteriaItems() {
            return successCriteriaItems;
        }

        public void setSuccessCriteriaItems(List<String> successCriteriaItems) {
            this.successCriteriaItems = successCriteriaItems;
        }

        public List<ProjectHealthIssueItem> getIssues() {
            return issues;
        }

        public void setIssues(List<ProjectHealthIssueItem> issues) {
            this.issues = issues;
        }

        public int getActionableIssueCount() {
            return actionableIssueCount;
        }

        public void setActionableIssueCount(int actionableIssueCount) {
            this.actionableIssueCount = actionableIssueCount;
        }
    }

    public static class ProjectHealthIssueItem {
        private String severity = "";
        private String title = "";
        private String detail = "";

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
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

    public static class ProposalActionItem {
        private int actionNextId;
        private String dateLabel = "";
        private String descriptionHtml = "";
        private String actionTypeLabel = "";
        private String estimateDisplay = "";

        public int getActionNextId() {
            return actionNextId;
        }

        public void setActionNextId(int actionNextId) {
            this.actionNextId = actionNextId;
        }

        public String getDateLabel() {
            return dateLabel;
        }

        public void setDateLabel(String dateLabel) {
            this.dateLabel = dateLabel;
        }

        public String getDescriptionHtml() {
            return descriptionHtml;
        }

        public void setDescriptionHtml(String descriptionHtml) {
            this.descriptionHtml = descriptionHtml;
        }

        public String getActionTypeLabel() {
            return actionTypeLabel;
        }

        public void setActionTypeLabel(String actionTypeLabel) {
            this.actionTypeLabel = actionTypeLabel;
        }

        public String getEstimateDisplay() {
            return estimateDisplay;
        }

        public void setEstimateDisplay(String estimateDisplay) {
            this.estimateDisplay = estimateDisplay;
        }
    }

    public static class NarrativeItem {
        private String dateLabel = "";
        private String authorLabel = "";
        private String verbLabel = "";
        private String text = "";

        public String getDateLabel() {
            return dateLabel;
        }

        public void setDateLabel(String dateLabel) {
            this.dateLabel = dateLabel;
        }

        public String getAuthorLabel() {
            return authorLabel;
        }

        public void setAuthorLabel(String authorLabel) {
            this.authorLabel = authorLabel;
        }

        public String getVerbLabel() {
            return verbLabel;
        }

        public void setVerbLabel(String verbLabel) {
            this.verbLabel = verbLabel;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}