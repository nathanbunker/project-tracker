package org.dandeliondaily.planahead.model;

import java.util.ArrayList;
import java.util.List;

import org.dandeliondaily.dashboard.model.TimeGaugeModel;

public class PlanAheadBoardModel {

    private String mode = "WORK";
    private String windowStartKey = "";
    private String windowEndKey = "";
    private String quickCaptureSentenceValue = "";
    private boolean quickCaptureFocusRequested;
    private List<String> quickCaptureProjectNames = new ArrayList<String>();
    private List<DayHeaderModel> dayHeaders = new ArrayList<DayHeaderModel>();
    private List<RowModel> rows = new ArrayList<RowModel>();
    private TemplateRowModel templateRow = new TemplateRowModel();

    public String getWindowStartKey() {
        return windowStartKey;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode == null ? "WORK" : mode;
    }

    public boolean isPersonalMode() {
        return "PERSONAL".equalsIgnoreCase(mode);
    }

    public boolean isWorkMode() {
        return !isPersonalMode();
    }

    public void setWindowStartKey(String windowStartKey) {
        this.windowStartKey = windowStartKey;
    }

    public String getWindowEndKey() {
        return windowEndKey;
    }

    public void setWindowEndKey(String windowEndKey) {
        this.windowEndKey = windowEndKey;
    }

    public String getQuickCaptureSentenceValue() {
        return quickCaptureSentenceValue;
    }

    public void setQuickCaptureSentenceValue(String quickCaptureSentenceValue) {
        this.quickCaptureSentenceValue = quickCaptureSentenceValue;
    }

    public boolean isQuickCaptureFocusRequested() {
        return quickCaptureFocusRequested;
    }

    public void setQuickCaptureFocusRequested(boolean quickCaptureFocusRequested) {
        this.quickCaptureFocusRequested = quickCaptureFocusRequested;
    }

    public List<String> getQuickCaptureProjectNames() {
        return quickCaptureProjectNames;
    }

    public void setQuickCaptureProjectNames(List<String> quickCaptureProjectNames) {
        this.quickCaptureProjectNames = quickCaptureProjectNames;
    }

    public List<DayHeaderModel> getDayHeaders() {
        return dayHeaders;
    }

    public void setDayHeaders(List<DayHeaderModel> dayHeaders) {
        this.dayHeaders = dayHeaders;
    }

    public List<RowModel> getRows() {
        return rows;
    }

    public void setRows(List<RowModel> rows) {
        this.rows = rows;
    }

    public TemplateRowModel getTemplateRow() {
        return templateRow;
    }

    public void setTemplateRow(TemplateRowModel templateRow) {
        this.templateRow = templateRow;
    }

    public static class DayHeaderModel {
        private String dayKey = "";
        private String dayLabel = "";
        private String dateLabel = "";
        private String workStatusCode = "";
        private String workStatusLabel = "";
        private int billMins;
        private int plannedMins;
        private int availableMins;
        private boolean weekend;
        private TimeGaugeModel gauge = new TimeGaugeModel();

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

        public String getDateLabel() {
            return dateLabel;
        }

        public void setDateLabel(String dateLabel) {
            this.dateLabel = dateLabel;
        }

        public String getWorkStatusCode() {
            return workStatusCode;
        }

        public void setWorkStatusCode(String workStatusCode) {
            this.workStatusCode = workStatusCode;
        }

        public String getWorkStatusLabel() {
            return workStatusLabel;
        }

        public void setWorkStatusLabel(String workStatusLabel) {
            this.workStatusLabel = workStatusLabel;
        }

        public int getBillMins() {
            return billMins;
        }

        public void setBillMins(int billMins) {
            this.billMins = billMins;
        }

        public int getPlannedMins() {
            return plannedMins;
        }

        public void setPlannedMins(int plannedMins) {
            this.plannedMins = plannedMins;
        }

        public int getAvailableMins() {
            return availableMins;
        }

        public void setAvailableMins(int availableMins) {
            this.availableMins = availableMins;
        }

        public boolean isWeekend() {
            return weekend;
        }

        public void setWeekend(boolean weekend) {
            this.weekend = weekend;
        }

        public TimeGaugeModel getGauge() {
            return gauge;
        }

        public void setGauge(TimeGaugeModel gauge) {
            this.gauge = gauge;
        }
    }

    public static class RowModel {
        private String rowKey = "";
        private String rowLabel = "";
        private List<CellModel> cells = new ArrayList<CellModel>();

        public String getRowKey() {
            return rowKey;
        }

        public void setRowKey(String rowKey) {
            this.rowKey = rowKey;
        }

        public String getRowLabel() {
            return rowLabel;
        }

        public void setRowLabel(String rowLabel) {
            this.rowLabel = rowLabel;
        }

        public List<CellModel> getCells() {
            return cells;
        }

        public void setCells(List<CellModel> cells) {
            this.cells = cells;
        }
    }

    public static class CellModel {
        private String dayKey = "";
        private String rowKey = "";
        private List<CardModel> cards = new ArrayList<CardModel>();

        public String getDayKey() {
            return dayKey;
        }

        public void setDayKey(String dayKey) {
            this.dayKey = dayKey;
        }

        public String getRowKey() {
            return rowKey;
        }

        public void setRowKey(String rowKey) {
            this.rowKey = rowKey;
        }

        public List<CardModel> getCards() {
            return cards;
        }

        public void setCards(List<CardModel> cards) {
            this.cards = cards;
        }
    }

    public static class CardModel {
        private int actionNextId;
        private String projectName = "";
        private String description = "";
        private String rawDescription = "";
        private int estimateMins;
        private String estimateDisplay = "";
        private String nextActionType = "";

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getRawDescription() {
            return rawDescription;
        }

        public void setRawDescription(String rawDescription) {
            this.rawDescription = rawDescription;
        }

        public int getEstimateMins() {
            return estimateMins;
        }

        public void setEstimateMins(int estimateMins) {
            this.estimateMins = estimateMins;
        }

        public String getEstimateDisplay() {
            return estimateDisplay;
        }

        public void setEstimateDisplay(String estimateDisplay) {
            this.estimateDisplay = estimateDisplay;
        }

        public String getNextActionType() {
            return nextActionType;
        }

        public void setNextActionType(String nextActionType) {
            this.nextActionType = nextActionType;
        }
    }

    public static class TemplateRowModel {
        private List<TemplateCardModel> templateCards = new ArrayList<TemplateCardModel>();

        public List<TemplateCardModel> getTemplateCards() {
            return templateCards;
        }

        public void setTemplateCards(List<TemplateCardModel> templateCards) {
            this.templateCards = templateCards;
        }
    }

    public static class TemplateCardModel {
        private int templateActionNextId;
        private String projectName = "";
        private String description = "";
        private int estimateMins;
        private String estimateDisplay = "";
        private String templateTypeLabel = "";
        private String templateTypeId = "";
        private List<TemplateDaySelectionModel> daySelections = new ArrayList<TemplateDaySelectionModel>();

        public int getTemplateActionNextId() {
            return templateActionNextId;
        }

        public void setTemplateActionNextId(int templateActionNextId) {
            this.templateActionNextId = templateActionNextId;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getEstimateMins() {
            return estimateMins;
        }

        public void setEstimateMins(int estimateMins) {
            this.estimateMins = estimateMins;
        }

        public String getEstimateDisplay() {
            return estimateDisplay;
        }

        public void setEstimateDisplay(String estimateDisplay) {
            this.estimateDisplay = estimateDisplay;
        }

        public String getTemplateTypeLabel() {
            return templateTypeLabel;
        }

        public void setTemplateTypeLabel(String templateTypeLabel) {
            this.templateTypeLabel = templateTypeLabel;
        }

        public String getTemplateTypeId() {
            return templateTypeId;
        }

        public void setTemplateTypeId(String templateTypeId) {
            this.templateTypeId = templateTypeId;
        }

        public List<TemplateDaySelectionModel> getDaySelections() {
            return daySelections;
        }

        public void setDaySelections(List<TemplateDaySelectionModel> daySelections) {
            this.daySelections = daySelections;
        }
    }

    public static class TemplateDaySelectionModel {
        private String dayKey = "";
        private String dayLabel = "";
        private boolean selected;

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

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}
