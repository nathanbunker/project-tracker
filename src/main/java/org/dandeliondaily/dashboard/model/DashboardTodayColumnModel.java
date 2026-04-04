package org.dandeliondaily.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class DashboardTodayColumnModel {

    private QuickCaptureModel quickCapture = new QuickCaptureModel();
    private TodayTotalsModel totals = new TodayTotalsModel();
    private List<TodayActionGroupModel> actionGroups = new ArrayList<TodayActionGroupModel>();
    private List<TodayActionItemModel> completedToday = new ArrayList<TodayActionItemModel>();
    private WorkdayReviewModel workdayReview = new WorkdayReviewModel();

    public QuickCaptureModel getQuickCapture() {
        return quickCapture;
    }

    public void setQuickCapture(QuickCaptureModel quickCapture) {
        this.quickCapture = quickCapture;
    }

    public TodayTotalsModel getTotals() {
        return totals;
    }

    public void setTotals(TodayTotalsModel totals) {
        this.totals = totals;
    }

    public List<TodayActionGroupModel> getActionGroups() {
        return actionGroups;
    }

    public void setActionGroups(List<TodayActionGroupModel> actionGroups) {
        this.actionGroups = actionGroups;
    }

    public List<TodayActionItemModel> getCompletedToday() {
        return completedToday;
    }

    public void setCompletedToday(List<TodayActionItemModel> completedToday) {
        this.completedToday = completedToday;
    }

    public WorkdayReviewModel getWorkdayReview() {
        return workdayReview;
    }

    public void setWorkdayReview(WorkdayReviewModel workdayReview) {
        this.workdayReview = workdayReview;
    }

    public static class QuickCaptureModel {
        private String formAction = "DandelionDashboardServlet";
        private String sentenceInputName = "sentenceInput";
        private String actionParamName = "action";
        private String scheduleActionValue = "Schedule";
        private String scheduleAndStartActionValue = "Schedule and Start";
        private String sentenceValue = "";
        private String placeholder = "Project Name: I will action details";
        private String hintText = "Example: Client Project: I will review deployment plan tomorrow";
        private List<String> projectNames = new ArrayList<String>();
        private boolean focusRequested = false;

        public String getFormAction() {
            return formAction;
        }

        public String getSentenceInputName() {
            return sentenceInputName;
        }

        public String getActionParamName() {
            return actionParamName;
        }

        public String getScheduleActionValue() {
            return scheduleActionValue;
        }

        public String getScheduleAndStartActionValue() {
            return scheduleAndStartActionValue;
        }

        public String getSentenceValue() {
            return sentenceValue;
        }

        public void setSentenceValue(String sentenceValue) {
            this.sentenceValue = sentenceValue;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public String getHintText() {
            return hintText;
        }

        public List<String> getProjectNames() {
            return projectNames;
        }

        public void setProjectNames(List<String> projectNames) {
            this.projectNames = projectNames;
        }

        public boolean isFocusRequested() {
            return focusRequested;
        }

        public void setFocusRequested(boolean focusRequested) {
            this.focusRequested = focusRequested;
        }
    }

    public static class TodayTotalsModel {
        private String completedDisplay = "-";
        private String committedDisplay = "-";
        private String willDisplay = "-";
        private String willMeetDisplay = "-";
        private String totalPlannedDisplay = "-";
        private String guidanceMessage = "";
        private boolean overCommitted;
        private int plannedMinutes = 0;
        private int completedMinutes = 0;

        public String getCompletedDisplay() {
            return completedDisplay;
        }

        public void setCompletedDisplay(String completedDisplay) {
            this.completedDisplay = completedDisplay;
        }

        public String getCommittedDisplay() {
            return committedDisplay;
        }

        public void setCommittedDisplay(String committedDisplay) {
            this.committedDisplay = committedDisplay;
        }

        public String getWillDisplay() {
            return willDisplay;
        }

        public void setWillDisplay(String willDisplay) {
            this.willDisplay = willDisplay;
        }

        public String getWillMeetDisplay() {
            return willMeetDisplay;
        }

        public void setWillMeetDisplay(String willMeetDisplay) {
            this.willMeetDisplay = willMeetDisplay;
        }

        public String getTotalPlannedDisplay() {
            return totalPlannedDisplay;
        }

        public void setTotalPlannedDisplay(String totalPlannedDisplay) {
            this.totalPlannedDisplay = totalPlannedDisplay;
        }

        public String getGuidanceMessage() {
            return guidanceMessage;
        }

        public void setGuidanceMessage(String guidanceMessage) {
            this.guidanceMessage = guidanceMessage;
        }

        public boolean isOverCommitted() {
            return overCommitted;
        }

        public void setOverCommitted(boolean overCommitted) {
            this.overCommitted = overCommitted;
        }

        public int getPlannedMinutes() {
            return plannedMinutes;
        }

        public void setPlannedMinutes(int plannedMinutes) {
            this.plannedMinutes = plannedMinutes;
        }

        public int getCompletedMinutes() {
            return completedMinutes;
        }

        public void setCompletedMinutes(int completedMinutes) {
            this.completedMinutes = completedMinutes;
        }
    }

    public static class TodayActionGroupModel {
        private String title = "";
        private List<TodayActionItemModel> items = new ArrayList<TodayActionItemModel>();

        public TodayActionGroupModel() {
        }

        public TodayActionGroupModel(String title, List<TodayActionItemModel> items) {
            this.title = title;
            this.items = items;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<TodayActionItemModel> getItems() {
            return items;
        }

        public void setItems(List<TodayActionItemModel> items) {
            this.items = items;
        }
    }

    public static class TodayActionItemModel {
        private int actionNextId;
        private String projectName = "";
        private String descriptionText = "";
        private String descriptionHtml = "";
        private String estimateDisplay = "";
        private String actualDisplay = "";
        private int estimateMinutes;
        private int actualMinutes;
        private String contextLabel = "";
        private String statusLabel = "";

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getDescriptionHtml() {
            return descriptionHtml;
        }

        public void setDescriptionHtml(String descriptionHtml) {
            this.descriptionHtml = descriptionHtml;
        }

        public String getDescriptionText() {
            return descriptionText;
        }

        public void setDescriptionText(String descriptionText) {
            this.descriptionText = descriptionText;
        }

        public String getEstimateDisplay() {
            return estimateDisplay;
        }

        public void setEstimateDisplay(String estimateDisplay) {
            this.estimateDisplay = estimateDisplay;
        }

        public String getActualDisplay() {
            return actualDisplay;
        }

        public void setActualDisplay(String actualDisplay) {
            this.actualDisplay = actualDisplay;
        }

        public int getEstimateMinutes() {
            return estimateMinutes;
        }

        public void setEstimateMinutes(int estimateMinutes) {
            this.estimateMinutes = estimateMinutes;
        }

        public int getActualMinutes() {
            return actualMinutes;
        }

        public void setActualMinutes(int actualMinutes) {
            this.actualMinutes = actualMinutes;
        }

        public String getContextLabel() {
            return contextLabel;
        }

        public void setContextLabel(String contextLabel) {
            this.contextLabel = contextLabel;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel;
        }

        public int getActionNextId() {
            return actionNextId;
        }

        public void setActionNextId(int actionNextId) {
            this.actionNextId = actionNextId;
        }
    }

    public static class WorkdayReviewModel {
        private boolean renderSection;
        private boolean allReviewed;
        private boolean topPriority;
        private List<WorkdayReviewItemModel> projectItems = new ArrayList<WorkdayReviewItemModel>();

        public boolean isRenderSection() {
            return renderSection;
        }

        public void setRenderSection(boolean renderSection) {
            this.renderSection = renderSection;
        }

        public boolean isAllReviewed() {
            return allReviewed;
        }

        public void setAllReviewed(boolean allReviewed) {
            this.allReviewed = allReviewed;
        }

        public boolean isTopPriority() {
            return topPriority;
        }

        public void setTopPriority(boolean topPriority) {
            this.topPriority = topPriority;
        }

        public List<WorkdayReviewItemModel> getProjectItems() {
            return projectItems;
        }

        public void setProjectItems(List<WorkdayReviewItemModel> projectItems) {
            this.projectItems = projectItems;
        }
    }

    public static class WorkdayReviewItemModel {
        private long projectId;
        private String projectName = "";
        private int completedCount;
        private int minutesSpent;
        private String minutesDisplay = "-";
        private boolean reviewed;
        private String note = "";
        private String decision = "";
        private String insight = "";
        private String risk = "";
        private String opportunity = "";
        private java.util.List<String> completedActions = new java.util.ArrayList<>();

        public long getProjectId() {
            return projectId;
        }

        public void setProjectId(long projectId) {
            this.projectId = projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public int getCompletedCount() {
            return completedCount;
        }

        public void setCompletedCount(int completedCount) {
            this.completedCount = completedCount;
        }

        public int getMinutesSpent() {
            return minutesSpent;
        }

        public void setMinutesSpent(int minutesSpent) {
            this.minutesSpent = minutesSpent;
        }

        public String getMinutesDisplay() {
            return minutesDisplay;
        }

        public void setMinutesDisplay(String minutesDisplay) {
            this.minutesDisplay = minutesDisplay;
        }

        public boolean isReviewed() {
            return reviewed;
        }

        public void setReviewed(boolean reviewed) {
            this.reviewed = reviewed;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public String getInsight() {
            return insight;
        }

        public void setInsight(String insight) {
            this.insight = insight;
        }

        public String getRisk() {
            return risk;
        }

        public void setRisk(String risk) {
            this.risk = risk;
        }

        public String getOpportunity() {
            return opportunity;
        }

        public void setOpportunity(String opportunity) {
            this.opportunity = opportunity;
        }

        public java.util.List<String> getCompletedActions() {
            return completedActions;
        }

        public void setCompletedActions(java.util.List<String> completedActions) {
            this.completedActions = completedActions;
        }
    }
}