package org.openimmunizationsoftware.pt.billing;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ReviewInterval;

public class BillingProjectReportFormatter {

    public static class ProjectReportItem {
        private Project project;
        private List<String> tags = new ArrayList<String>();
        private int updateEveryDays;

        public Project getProject() {
            return project;
        }

        public void setProject(Project project) {
            this.project = project;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags == null ? new ArrayList<String>() : tags;
        }

        public int getUpdateEveryDays() {
            return updateEveryDays;
        }

        public void setUpdateEveryDays(int updateEveryDays) {
            this.updateEveryDays = updateEveryDays;
        }
    }

    public String formatReport(String billCode, List<ProjectReportItem> items) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Billing Project Report: ").append(singleLine(billCode)).append("\n\n");
        if (items == null || items.isEmpty()) {
            markdown.append("No projects found.\n");
            return markdown.toString();
        }
        for (ProjectReportItem item : items) {
            markdown.append(formatProject(item)).append("\n");
        }
        return markdown.toString();
    }

    public String formatProject(ProjectReportItem item) {
        Project project = item == null ? null : item.getProject();
        if (project == null) {
            return "";
        }
        StringBuilder markdown = new StringBuilder();
        markdown.append("## ").append(singleLine(project.getProjectName())).append("\n\n");
        appendField(markdown, "Project Name", project.getProjectName());
        appendField(markdown, "Project Handle", project.getProjectHandle());
        appendField(markdown, "Tags", joinTags(item.getTags()));
        appendSection(markdown, "Description", project.getDescription());
        appendSection(markdown, "Current Focus", project.getCurrentFocusText());
        appendSection(markdown, "Project Outcome", project.getOutcomeText());
        appendSection(markdown, "Success Criteria", project.getSuccessCriteriaText());
        appendField(markdown, "Status", project.getProjectStatus());
        appendField(markdown, "Bill Code", project.getBillCode());
        appendField(markdown, "Update Every", ReviewInterval.makeLabel(item.getUpdateEveryDays()));
        return markdown.toString();
    }

    private void appendField(StringBuilder markdown, String label, String value) {
        markdown.append("- **").append(label).append(":** ").append(display(singleLine(value))).append("\n");
    }

    private void appendSection(StringBuilder markdown, String label, String value) {
        markdown.append("\n### ").append(label).append("\n\n").append(display(value)).append("\n");
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        StringBuilder joined = new StringBuilder();
        for (String tag : tags) {
            if (joined.length() > 0) {
                joined.append(", ");
            }
            joined.append(singleLine(tag));
        }
        return joined.toString();
    }

    private String display(String value) {
        return value == null || value.trim().length() == 0 ? "_(not provided)_" : value.trim();
    }

    private String singleLine(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}