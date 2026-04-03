package org.dandeliondaily.dashboard.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DashboardNowColumnModel {

    private CurrentAction currentAction = new CurrentAction();
    private CurrentProject currentProject = new CurrentProject();
    private List<ScheduledActionItem> scheduledActions = new ArrayList<>();
    private List<UnscheduledActionItem> unscheduledActions = new ArrayList<>();
    private List<TemplatedActionItem> templatedActions = new ArrayList<>();
    private List<RecentCompletedItem> recentCompleted = new ArrayList<>();
    private List<OpenIssueItem> openIssues = new ArrayList<>();

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
}