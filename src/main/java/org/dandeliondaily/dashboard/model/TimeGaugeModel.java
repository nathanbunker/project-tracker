package org.dandeliondaily.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class TimeGaugeModel {

    public static class GaugeRow {
        private final String label;
        private final int currentMinutes;
        private final int targetMinutes;
        private TimeGaugeState state = TimeGaugeState.NORMAL;

        public GaugeRow(String label, int currentMinutes, int targetMinutes) {
            this.label = label;
            this.currentMinutes = currentMinutes;
            this.targetMinutes = targetMinutes;
        }

        public String getLabel() {
            return label;
        }

        public int getCurrentMinutes() {
            return currentMinutes;
        }

        public int getTargetMinutes() {
            return targetMinutes;
        }

        public TimeGaugeState getState() {
            return state;
        }

        public void setState(TimeGaugeState state) {
            this.state = state;
        }
    }

    private TimeGaugeVariant variant = TimeGaugeVariant.STACKED;
    private String title;
    private int currentMinutes;
    private int targetMinutes;
    private String statusText = "";
    private boolean showTitle = true;
    private boolean showStatus = true;
    private TimeGaugeState state = TimeGaugeState.UNKNOWN;
    private List<GaugeRow> rows = new ArrayList<GaugeRow>();

    public TimeGaugeVariant getVariant() {
        return variant;
    }

    public void setVariant(TimeGaugeVariant variant) {
        this.variant = variant;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCurrentMinutes() {
        return currentMinutes;
    }

    public void setCurrentMinutes(int currentMinutes) {
        this.currentMinutes = currentMinutes;
    }

    public int getTargetMinutes() {
        return targetMinutes;
    }

    public void setTargetMinutes(int targetMinutes) {
        this.targetMinutes = targetMinutes;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    public boolean isShowStatus() {
        return showStatus;
    }

    public void setShowStatus(boolean showStatus) {
        this.showStatus = showStatus;
    }

    public TimeGaugeState getState() {
        return state;
    }

    public void setState(TimeGaugeState state) {
        this.state = state;
    }

    public List<GaugeRow> getRows() {
        return rows;
    }

    public void addRow(GaugeRow row) {
        this.rows.add(row);
    }

    public boolean hasTitle() {
        return title != null && title.trim().length() > 0;
    }
}