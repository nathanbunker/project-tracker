package org.openimmunizationsoftware.pt.schooltemplate;

import java.util.List;

import org.openimmunizationsoftware.pt.model.Project;

/** One resolved, DB-validated change (add/update/delete) computed from an import entry, ready to preview or apply. */
public class TemplateChange {

    public enum ChangeType {
        ADD, UPDATE, DELETE
    }

    private final ChangeType changeType;
    private final SchoolTemplateImportEntry sourceEntry;
    private final SchoolTemplateSnapshot existingSnapshot;
    private final Project resolvedProject;
    private final boolean billable;
    private final List<FieldDiff> fieldDiffs;

    public TemplateChange(ChangeType changeType, SchoolTemplateImportEntry sourceEntry,
            SchoolTemplateSnapshot existingSnapshot, Project resolvedProject, boolean billable,
            List<FieldDiff> fieldDiffs) {
        this.changeType = changeType;
        this.sourceEntry = sourceEntry;
        this.existingSnapshot = existingSnapshot;
        this.resolvedProject = resolvedProject;
        this.billable = billable;
        this.fieldDiffs = fieldDiffs;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public SchoolTemplateImportEntry getSourceEntry() {
        return sourceEntry;
    }

    public SchoolTemplateSnapshot getExistingSnapshot() {
        return existingSnapshot;
    }

    public Project getResolvedProject() {
        return resolvedProject;
    }

    public boolean isBillable() {
        return billable;
    }

    public List<FieldDiff> getFieldDiffs() {
        return fieldDiffs;
    }

    public String getDisplayDescription() {
        if (sourceEntry != null && sourceEntry.getDescription() != null) {
            return sourceEntry.getDescription();
        }
        if (existingSnapshot != null) {
            return existingSnapshot.getActionNext().getNextDescription();
        }
        return "";
    }

    public String getDisplayProjectName() {
        if (resolvedProject != null) {
            return resolvedProject.getProjectName();
        }
        if (existingSnapshot != null && existingSnapshot.getProject() != null) {
            return existingSnapshot.getProject().getProjectName();
        }
        return "";
    }
}
