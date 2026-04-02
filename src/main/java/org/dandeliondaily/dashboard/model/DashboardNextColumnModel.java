package org.dandeliondaily.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class DashboardNextColumnModel {

    private static final String SELECTED_DAY_PARAM = "selectedDay";

    private List<NextDaySummaryModel> daySummaries = new ArrayList<NextDaySummaryModel>();
    private SelectedDayModel selectedDay = new SelectedDayModel();

    public List<NextDaySummaryModel> getDaySummaries() {
        return daySummaries;
    }

    public void setDaySummaries(List<NextDaySummaryModel> daySummaries) {
        this.daySummaries = daySummaries;
    }

    public SelectedDayModel getSelectedDay() {
        return selectedDay;
    }

    public void setSelectedDay(SelectedDayModel selectedDay) {
        this.selectedDay = selectedDay;
    }

    public static String getSelectedDayParam() {
        return SELECTED_DAY_PARAM;
    }

    public static class NextDaySummaryModel {
        private String dayKey = "";
        private String dayLabel = "";
        private String dayShortLabel = "";
        private String fullDateLabel = "";
        private String plannedDisplay = "";
        private String committedDisplay = "";
        private String willDisplay = "";
        private String willMeetDisplay = "";
        private int plannedMinutes = 0;
        private boolean selected;
        private TimeGaugeModel inlineGauge = new TimeGaugeModel();

        public String getDayKey() {
            return dayKey;
        }

        public void setDayKey(String dayKey) {
            this.dayKey = dayKey;
        }

        public String getDayLabel() {
            return dayLabel;
        }

        public void setDayLabel(String dayLabel) {
            this.dayLabel = dayLabel;
        }

        public String getDayShortLabel() {
            return dayShortLabel;
        }

        public void setDayShortLabel(String dayShortLabel) {
            this.dayShortLabel = dayShortLabel;
        }

        public String getFullDateLabel() {
            return fullDateLabel;
        }

        public void setFullDateLabel(String fullDateLabel) {
            this.fullDateLabel = fullDateLabel;
        }

        public String getPlannedDisplay() {
            return plannedDisplay;
        }

        public void setPlannedDisplay(String plannedDisplay) {
            this.plannedDisplay = plannedDisplay;
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

        public int getPlannedMinutes() {
            return plannedMinutes;
        }

        public void setPlannedMinutes(int plannedMinutes) {
            this.plannedMinutes = plannedMinutes;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public TimeGaugeModel getInlineGauge() {
            return inlineGauge;
        }

        public void setInlineGauge(TimeGaugeModel inlineGauge) {
            this.inlineGauge = inlineGauge;
        }
    }

    public static class SelectedDayModel {
        private String dayKey = "";
        private String dayLabel = "";
        private String fullDateLabel = "";
        private String plannedDisplay = "";
        private int plannedMinutes = 0;
        private TimeGaugeModel headerGauge = new TimeGaugeModel();
        private List<SelectedDaySectionModel> sections = new ArrayList<SelectedDaySectionModel>();

        public String getDayKey() {
            return dayKey;
        }

        public void setDayKey(String dayKey) {
            this.dayKey = dayKey;
        }

        public String getDayLabel() {
            return dayLabel;
        }

        public void setDayLabel(String dayLabel) {
            this.dayLabel = dayLabel;
        }

        public String getFullDateLabel() {
            return fullDateLabel;
        }

        public void setFullDateLabel(String fullDateLabel) {
            this.fullDateLabel = fullDateLabel;
        }

        public String getPlannedDisplay() {
            return plannedDisplay;
        }

        public void setPlannedDisplay(String plannedDisplay) {
            this.plannedDisplay = plannedDisplay;
        }

        public int getPlannedMinutes() {
            return plannedMinutes;
        }

        public void setPlannedMinutes(int plannedMinutes) {
            this.plannedMinutes = plannedMinutes;
        }

        public TimeGaugeModel getHeaderGauge() {
            return headerGauge;
        }

        public void setHeaderGauge(TimeGaugeModel headerGauge) {
            this.headerGauge = headerGauge;
        }

        public List<SelectedDaySectionModel> getSections() {
            return sections;
        }

        public void setSections(List<SelectedDaySectionModel> sections) {
            this.sections = sections;
        }
    }

    public static class SelectedDaySectionModel {
        private String title = "";
        private List<SelectedDayActionItemModel> items = new ArrayList<SelectedDayActionItemModel>();

        public SelectedDaySectionModel() {
        }

        public SelectedDaySectionModel(String title, List<SelectedDayActionItemModel> items) {
            this.title = title;
            this.items = items;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<SelectedDayActionItemModel> getItems() {
            return items;
        }

        public void setItems(List<SelectedDayActionItemModel> items) {
            this.items = items;
        }
    }

    public static class SelectedDayActionItemModel {
        private int actionNextId;
        private String projectName = "";
        private String descriptionHtml = "";
        private String descriptionPlain = "";
        private String estimateDisplay = "";
        private String actualDisplay = "";
        private String statusLabel = "";

        public int getActionNextId() {
            return actionNextId;
        }

        public void setActionNextId(int actionNextId) {
            this.actionNextId = actionNextId;
        }

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

        public String getDescriptionPlain() {
            return descriptionPlain;
        }

        public void setDescriptionPlain(String descriptionPlain) {
            this.descriptionPlain = descriptionPlain;
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

        public String getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel;
        }
    }
}
