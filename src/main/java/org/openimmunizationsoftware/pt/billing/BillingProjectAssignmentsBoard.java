package org.openimmunizationsoftware.pt.billing;

import java.util.ArrayList;
import java.util.List;

public class BillingProjectAssignmentsBoard {

    private final List<BillCodeColumn> columns = new ArrayList<BillCodeColumn>();
    private final List<CadenceRow> rows = new ArrayList<CadenceRow>();

    public List<BillCodeColumn> getColumns() {
        return columns;
    }

    public List<CadenceRow> getRows() {
        return rows;
    }

    public static class BillCodeColumn {
        private String billCode;
        private String label;

        public String getBillCode() {
            return billCode;
        }

        public void setBillCode(String billCode) {
            this.billCode = billCode;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class CadenceRow {
        private int updateEveryDays;
        private String label;
        private boolean legacy;
        private final List<AssignmentCell> cells = new ArrayList<AssignmentCell>();

        public int getUpdateEveryDays() {
            return updateEveryDays;
        }

        public void setUpdateEveryDays(int updateEveryDays) {
            this.updateEveryDays = updateEveryDays;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public boolean isLegacy() {
            return legacy;
        }

        public void setLegacy(boolean legacy) {
            this.legacy = legacy;
        }

        public List<AssignmentCell> getCells() {
            return cells;
        }
    }

    public static class AssignmentCell {
        private String billCode;
        private int updateEveryDays;
        private final List<ProjectChip> projects = new ArrayList<ProjectChip>();

        public String getBillCode() {
            return billCode;
        }

        public void setBillCode(String billCode) {
            this.billCode = billCode;
        }

        public int getUpdateEveryDays() {
            return updateEveryDays;
        }

        public void setUpdateEveryDays(int updateEveryDays) {
            this.updateEveryDays = updateEveryDays;
        }

        public List<ProjectChip> getProjects() {
            return projects;
        }
    }

    public static class ProjectChip {
        private int projectId;
        private String projectName;

        public int getProjectId() {
            return projectId;
        }

        public void setProjectId(int projectId) {
            this.projectId = projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }
    }
}