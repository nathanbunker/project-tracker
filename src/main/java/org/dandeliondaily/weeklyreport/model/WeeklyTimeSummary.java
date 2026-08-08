package org.dandeliondaily.weeklyreport.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WeeklyTimeSummary {
    private final LocalDate weekStart;
    private int allWorkedMinutes;
    private int scopedMinutes;
    private final Map<String, Integer> minutesByBillCode = new LinkedHashMap<String, Integer>();
    private final List<ProjectTime> projects = new ArrayList<ProjectTime>();

    public WeeklyTimeSummary(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public int getAllWorkedMinutes() {
        return allWorkedMinutes;
    }

    public void setAllWorkedMinutes(int allWorkedMinutes) {
        this.allWorkedMinutes = allWorkedMinutes;
    }

    public int getScopedMinutes() {
        return scopedMinutes;
    }

    public void setScopedMinutes(int scopedMinutes) {
        this.scopedMinutes = scopedMinutes;
    }

    public Map<String, Integer> getMinutesByBillCode() {
        return minutesByBillCode;
    }

    public List<ProjectTime> getProjects() {
        return projects;
    }

    public void sort() {
        Collections.sort(projects, new Comparator<ProjectTime>() {
            public int compare(ProjectTime left, ProjectTime right) {
                int minutes = right.getRoundedMinutes() - left.getRoundedMinutes();
                return minutes != 0 ? minutes : left.getProjectName().compareToIgnoreCase(right.getProjectName());
            }
        });
    }

    public static class ProjectTime {
        private final int projectId;
        private final String projectName;
        private final String projectHandle;
        private final String billCode;
        private final int rawMinutes;
        private final int roundedMinutes;

        public ProjectTime(int projectId, String projectName, String projectHandle, String billCode,
                int rawMinutes, int roundedMinutes) {
            this.projectId = projectId;
            this.projectName = projectName == null ? "" : projectName;
            this.projectHandle = projectHandle;
            this.billCode = billCode;
            this.rawMinutes = rawMinutes;
            this.roundedMinutes = roundedMinutes;
        }

        public int getProjectId() {
            return projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public String getProjectHandle() {
            return projectHandle;
        }

        public String getBillCode() {
            return billCode;
        }

        public int getRawMinutes() {
            return rawMinutes;
        }

        public int getRoundedMinutes() {
            return roundedMinutes;
        }
    }
}