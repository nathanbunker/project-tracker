package org.dandeliondaily.weeklyreport.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

public class WeeklyReportViewModel {
    private WeeklyReport report;
    private LocalDate weekStart;
    private WeeklyTimeSummary selectedWeek;
    private String approvedNarrativeHtml;
    private int fourWeekAllWorkedMinutes;
    private int fiscalYearAllWorkedMinutes;
    private int obligatedMinutes;
    private boolean obligatedIsDefault = true;
    private String obligatedNote;
    private int fourWeekObligatedMinutes;
    private int fiscalYearObligatedMinutes;
    private final List<WeeklyTimeSummary> history = new ArrayList<WeeklyTimeSummary>();
    private final Map<LocalDate, Integer> obligatedMinutesByWeek = new LinkedHashMap<LocalDate, Integer>();
    private final Map<String, AllocationRow> allocationRows = new LinkedHashMap<String, AllocationRow>();
    private final Map<String, ProjectActivity> projectActivities = new LinkedHashMap<String, ProjectActivity>();
    private final Map<String, Integer> fiscalProjectMinutes = new LinkedHashMap<String, Integer>();

    public WeeklyReport getReport() {
        return report;
    }

    public void setReport(WeeklyReport report) {
        this.report = report;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public WeeklyTimeSummary getSelectedWeek() {
        return selectedWeek;
    }

    public void setSelectedWeek(WeeklyTimeSummary selectedWeek) {
        this.selectedWeek = selectedWeek;
    }

    public String getApprovedNarrativeHtml() {
        return approvedNarrativeHtml;
    }

    public void setApprovedNarrativeHtml(String approvedNarrativeHtml) {
        this.approvedNarrativeHtml = approvedNarrativeHtml;
    }

    public int getFourWeekAllWorkedMinutes() {
        return fourWeekAllWorkedMinutes;
    }

    public void setFourWeekAllWorkedMinutes(int fourWeekAllWorkedMinutes) {
        this.fourWeekAllWorkedMinutes = fourWeekAllWorkedMinutes;
    }

    public int getFiscalYearAllWorkedMinutes() {
        return fiscalYearAllWorkedMinutes;
    }

    public void setFiscalYearAllWorkedMinutes(int fiscalYearAllWorkedMinutes) {
        this.fiscalYearAllWorkedMinutes = fiscalYearAllWorkedMinutes;
    }

    public int getObligatedMinutes() {
        return obligatedMinutes;
    }

    public void setObligatedMinutes(int obligatedMinutes) {
        this.obligatedMinutes = obligatedMinutes;
    }

    public boolean isObligatedIsDefault() {
        return obligatedIsDefault;
    }

    public void setObligatedIsDefault(boolean obligatedIsDefault) {
        this.obligatedIsDefault = obligatedIsDefault;
    }

    public String getObligatedNote() {
        return obligatedNote;
    }

    public void setObligatedNote(String obligatedNote) {
        this.obligatedNote = obligatedNote;
    }

    public int getFourWeekObligatedMinutes() {
        return fourWeekObligatedMinutes;
    }

    public void setFourWeekObligatedMinutes(int fourWeekObligatedMinutes) {
        this.fourWeekObligatedMinutes = fourWeekObligatedMinutes;
    }

    public int getFiscalYearObligatedMinutes() {
        return fiscalYearObligatedMinutes;
    }

    public void setFiscalYearObligatedMinutes(int fiscalYearObligatedMinutes) {
        this.fiscalYearObligatedMinutes = fiscalYearObligatedMinutes;
    }

    public Map<LocalDate, Integer> getObligatedMinutesByWeek() {
        return obligatedMinutesByWeek;
    }

    public List<WeeklyTimeSummary> getHistory() {
        return history;
    }

    public Map<String, AllocationRow> getAllocationRows() {
        return allocationRows;
    }

    public Map<String, ProjectActivity> getProjectActivities() {
        return projectActivities;
    }

    public Map<String, Integer> getFiscalProjectMinutes() {
        return fiscalProjectMinutes;
    }

    public int getFiscalProjectMinutes(int projectId, String billCode) {
        Integer minutes = fiscalProjectMinutes.get(projectKey(projectId, billCode));
        return minutes == null ? 0 : minutes.intValue();
    }

    public static String projectKey(int projectId, String billCode) {
        return billCode + "\u0000" + projectId;
    }

    public static class AllocationRow {
        private String billCode;
        private String billLabel;
        private String fundingSource;
        private int weekMinutes;
        private int fourWeekMinutes;
        private int fiscalYearMinutes;
        private BigDecimal weekPercent;
        private BigDecimal fourWeekPercent;
        private BigDecimal fiscalYearPercent;
        private BigDecimal annualTargetPercent;
        private BigDecimal steeringTargetPercent;

        public String getBillCode() {
            return billCode;
        }

        public void setBillCode(String billCode) {
            this.billCode = billCode;
        }

        public String getBillLabel() {
            return billLabel;
        }

        public void setBillLabel(String billLabel) {
            this.billLabel = billLabel;
        }

        public String getFundingSource() {
            return fundingSource;
        }

        public void setFundingSource(String fundingSource) {
            this.fundingSource = fundingSource;
        }

        public int getWeekMinutes() {
            return weekMinutes;
        }

        public void setWeekMinutes(int weekMinutes) {
            this.weekMinutes = weekMinutes;
        }

        public int getFourWeekMinutes() {
            return fourWeekMinutes;
        }

        public void setFourWeekMinutes(int fourWeekMinutes) {
            this.fourWeekMinutes = fourWeekMinutes;
        }

        public int getFiscalYearMinutes() {
            return fiscalYearMinutes;
        }

        public void setFiscalYearMinutes(int fiscalYearMinutes) {
            this.fiscalYearMinutes = fiscalYearMinutes;
        }

        public BigDecimal getWeekPercent() {
            return weekPercent;
        }

        public void setWeekPercent(BigDecimal weekPercent) {
            this.weekPercent = weekPercent;
        }

        public BigDecimal getFourWeekPercent() {
            return fourWeekPercent;
        }

        public void setFourWeekPercent(BigDecimal fourWeekPercent) {
            this.fourWeekPercent = fourWeekPercent;
        }

        public BigDecimal getFiscalYearPercent() {
            return fiscalYearPercent;
        }

        public void setFiscalYearPercent(BigDecimal fiscalYearPercent) {
            this.fiscalYearPercent = fiscalYearPercent;
        }

        public BigDecimal getAnnualTargetPercent() {
            return annualTargetPercent;
        }

        public void setAnnualTargetPercent(BigDecimal annualTargetPercent) {
            this.annualTargetPercent = annualTargetPercent;
        }

        public BigDecimal getSteeringTargetPercent() {
            return steeringTargetPercent;
        }

        public void setSteeringTargetPercent(BigDecimal steeringTargetPercent) {
            this.steeringTargetPercent = steeringTargetPercent;
        }
    }

    public static class ProjectActivity {
        private int projectId;
        private Project project;
        private String billCode;
        private int roundedMinutes;
        private final List<ActivityItem> completedItems = new ArrayList<ActivityItem>();

        public int getProjectId() {
            return projectId;
        }

        public void setProjectId(int projectId) {
            this.projectId = projectId;
        }

        public Project getProject() {
            return project;
        }

        public void setProject(Project project) {
            this.project = project;
        }

        public String getBillCode() {
            return billCode;
        }

        public void setBillCode(String billCode) {
            this.billCode = billCode;
        }

        public int getRoundedMinutes() {
            return roundedMinutes;
        }

        public void setRoundedMinutes(int roundedMinutes) {
            this.roundedMinutes = roundedMinutes;
        }

        public List<ActivityItem> getCompletedItems() {
            return completedItems;
        }
    }

    public static class ActivityItem {
        private final Date date;
        private final String description;
        private final String notes;
        private final boolean completedAction;

        public ActivityItem(Date date, String description, String notes) {
            this(date, description, notes, false);
        }

        public ActivityItem(Date date, String description, String notes, boolean completedAction) {
            this.date = date;
            this.description = description;
            this.notes = notes;
            this.completedAction = completedAction;
        }

        public Date getDate() {
            return date;
        }

        public String getDescription() {
            return description;
        }

        public String getNotes() {
            return notes;
        }

        public boolean isCompletedAction() {
            return completedAction;
        }
    }
}