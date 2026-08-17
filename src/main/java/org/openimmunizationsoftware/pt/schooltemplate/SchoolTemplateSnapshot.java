package org.openimmunizationsoftware.pt.schooltemplate;

import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionNextTemplateConfig;
import org.openimmunizationsoftware.pt.model.Project;

/** A template row (ActionNext + its optional config), tagged with the project it belongs to and whether that project is billable ("School") or not ("Chores"). */
public class SchoolTemplateSnapshot {

    private final ActionNext actionNext;
    private final ActionNextTemplateConfig config;
    private final Project project;
    private final boolean billable;

    public SchoolTemplateSnapshot(ActionNext actionNext, ActionNextTemplateConfig config, Project project,
            boolean billable) {
        this.actionNext = actionNext;
        this.config = config;
        this.project = project;
        this.billable = billable;
    }

    public ActionNext getActionNext() {
        return actionNext;
    }

    public ActionNextTemplateConfig getConfig() {
        return config;
    }

    public Project getProject() {
        return project;
    }

    public boolean isBillable() {
        return billable;
    }

    public String getCategory() {
        return billable ? "School" : "Chores";
    }
}
