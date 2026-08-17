package org.openimmunizationsoftware.pt.schooltemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionNextTemplateConfig;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.TimeSlot;

/**
 * Resolves parsed {@link SchoolTemplateImportEntry} records against the current database state (project names,
 * existing template ids) and produces a list of {@link TemplateChange}s. Purely a read/compare step — never
 * mutates anything — so it can safely be called once to render a preview and again, fresh, right before commit.
 * <p>
 * Validation is all-or-nothing: every problem across every record is collected, and if any exist, nothing is
 * returned — the caller must fix the JSON and re-submit. This is deliberate: this data is edited outside the app
 * (round-tripped through a person and an LLM), so a partial apply would be confusing and risks acting on a
 * half-broken batch.
 */
public class SchoolTemplateDiffService {

    public List<TemplateChange> computeDiff(List<Project> workspaceProjects, Map<Integer, Boolean> projectBillableMap,
            List<SchoolTemplateSnapshot> existingSnapshots, List<SchoolTemplateImportEntry> importEntries) {

        Map<Integer, SchoolTemplateSnapshot> byId = new HashMap<Integer, SchoolTemplateSnapshot>();
        for (SchoolTemplateSnapshot snapshot : existingSnapshots) {
            byId.put(Integer.valueOf(snapshot.getActionNext().getActionNextId()), snapshot);
        }

        Map<String, List<Project>> projectsByName = new HashMap<String, List<Project>>();
        for (Project project : workspaceProjects) {
            String key = normalize(project.getProjectName());
            List<Project> matches = projectsByName.get(key);
            if (matches == null) {
                matches = new ArrayList<Project>();
                projectsByName.put(key, matches);
            }
            matches.add(project);
        }

        List<String> errors = new ArrayList<String>();
        Set<Integer> usedIds = new HashSet<Integer>();
        List<TemplateChange> changes = new ArrayList<TemplateChange>();

        for (SchoolTemplateImportEntry entry : importEntries) {
            if (entry.isDelete()) {
                if (!usedIds.add(entry.getId())) {
                    errors.add("Record " + entry.getRecordNumber() + ": id " + entry.getId()
                            + " is referenced more than once in this import.");
                    continue;
                }
                SchoolTemplateSnapshot existing = byId.get(entry.getId());
                if (existing == null) {
                    errors.add("Record " + entry.getRecordNumber() + ": template id " + entry.getId()
                            + " was not found (it may already be closed).");
                    continue;
                }
                changes.add(new TemplateChange(TemplateChange.ChangeType.DELETE, entry, existing,
                        existing.getProject(), existing.isBillable(), new ArrayList<FieldDiff>()));
                continue;
            }

            List<Project> matches = projectsByName.get(normalize(entry.getProject()));
            if (matches == null || matches.isEmpty()) {
                errors.add("Record " + entry.getRecordNumber() + ": project '" + entry.getProject()
                        + "' was not found.");
                continue;
            }
            if (matches.size() > 1) {
                errors.add("Record " + entry.getRecordNumber() + ": project '" + entry.getProject()
                        + "' matches more than one project.");
                continue;
            }
            Project project = matches.get(0);
            boolean billable = Boolean.TRUE.equals(projectBillableMap.get(Integer.valueOf(project.getProjectId())));

            if (entry.getId() == null) {
                changes.add(new TemplateChange(TemplateChange.ChangeType.ADD, entry, null, project, billable,
                        new ArrayList<FieldDiff>()));
                continue;
            }

            if (!usedIds.add(entry.getId())) {
                errors.add("Record " + entry.getRecordNumber() + ": id " + entry.getId()
                        + " is referenced more than once in this import.");
                continue;
            }
            SchoolTemplateSnapshot existing = byId.get(entry.getId());
            if (existing == null) {
                errors.add("Record " + entry.getRecordNumber() + ": template id " + entry.getId()
                        + " was not found for this dependent.");
                continue;
            }

            List<FieldDiff> fieldDiffs = buildFieldDiffs(existing, entry, project, billable);
            changes.add(new TemplateChange(TemplateChange.ChangeType.UPDATE, entry, existing, project, billable,
                    fieldDiffs));
        }

        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder();
            for (String error : errors) {
                if (message.length() > 0) {
                    message.append('\n');
                }
                message.append(error);
            }
            throw new IllegalArgumentException(message.toString());
        }

        return changes;
    }

    private List<FieldDiff> buildFieldDiffs(SchoolTemplateSnapshot existing, SchoolTemplateImportEntry entry,
            Project newProject, boolean newBillable) {
        List<FieldDiff> diffs = new ArrayList<FieldDiff>();
        ActionNext actionNext = existing.getActionNext();
        ActionNextTemplateConfig config = existing.getConfig();

        String oldProjectName = existing.getProject() == null ? "" : existing.getProject().getProjectName();
        String newProjectName = newProject.getProjectName();
        if (!eq(oldProjectName, newProjectName)) {
            diffs.add(new FieldDiff("Project", oldProjectName, newProjectName));
        }

        String oldDescription = safe(actionNext.getNextDescription());
        if (!eq(oldDescription, entry.getDescription())) {
            diffs.add(new FieldDiff("Description", oldDescription, entry.getDescription()));
        }

        TemplateType oldType = actionNext.getTemplateType() != null ? actionNext.getTemplateType() : TemplateType.DAILY;
        String oldScheduleLabel = scheduleLabel(oldType.name(), existingTokens(oldType, config));
        String newScheduleLabel = scheduleLabel(entry.getScheduleType(), entryTokens(entry));
        if (!eq(oldScheduleLabel, newScheduleLabel)) {
            diffs.add(new FieldDiff("Schedule", oldScheduleLabel, newScheduleLabel));
        }

        String oldMissed = config != null && !safe(config.getMissedActionBehavior()).isEmpty()
                ? config.getMissedActionBehavior() : "AUTO_CANCEL";
        if (!eq(oldMissed, entry.getMissedActionBehavior())) {
            diffs.add(new FieldDiff("Missed Behavior", oldMissed, entry.getMissedActionBehavior()));
        }

        boolean oldAutoGenerate = config == null || config.isAutoGenerate();
        boolean newAutoGenerate = entry.getAutoGenerate() == null || entry.getAutoGenerate().booleanValue();
        if (oldAutoGenerate != newAutoGenerate) {
            diffs.add(new FieldDiff("Auto Generate", oldAutoGenerate ? "Yes" : "No", newAutoGenerate ? "Yes" : "No"));
        }

        String oldActionType = safe(actionNext.getNextActionType());
        if (oldActionType.isEmpty()) {
            oldActionType = ProjectNextActionType.WILL;
        }
        if (!eq(oldActionType, entry.getActionType())) {
            diffs.add(new FieldDiff("Action Type", ProjectNextActionType.getLabel(oldActionType),
                    ProjectNextActionType.getLabel(entry.getActionType())));
        }

        if (newBillable) {
            int oldEstimate = actionNext.getNextTimeEstimate() == null ? 0 : actionNext.getNextTimeEstimate().intValue();
            int newEstimate = entry.getTimeEstimateMinutes() == null ? 0 : entry.getTimeEstimateMinutes().intValue();
            if (oldEstimate != newEstimate) {
                diffs.add(new FieldDiff("Time Estimate (mins)", String.valueOf(oldEstimate), String.valueOf(newEstimate)));
            }
            int oldPoints = actionNext.getGamePoints() == null ? 0 : actionNext.getGamePoints().intValue();
            int newPoints = entry.getGamePoints() == null ? 0 : entry.getGamePoints().intValue();
            if (oldPoints != newPoints) {
                diffs.add(new FieldDiff("Points", String.valueOf(oldPoints), String.valueOf(newPoints)));
            }
        } else {
            TimeSlot oldSlot = actionNext.getTimeSlot() != null ? actionNext.getTimeSlot() : TimeSlot.AFTERNOON;
            String newSlot = entry.getTimeSlot() != null ? entry.getTimeSlot() : TimeSlot.AFTERNOON.name();
            if (!eq(oldSlot.name(), newSlot)) {
                diffs.add(new FieldDiff("Time Slot", oldSlot.getLabel(), TimeSlot.valueOf(newSlot).getLabel()));
            }
        }

        return diffs;
    }

    private List<String> existingTokens(TemplateType type, ActionNextTemplateConfig config) {
        if (config == null) {
            return new ArrayList<String>();
        }
        String csv;
        switch (type) {
            case WEEKLY:
                csv = config.getScheduleDaysOfWeek();
                break;
            case MONTHLY:
                csv = config.getScheduleDaysOfMonth();
                break;
            case QUARTERLY:
                csv = config.getScheduleDaysOfQuarter();
                break;
            case YEARLY:
                csv = config.getScheduleDaysOfYear();
                break;
            default:
                csv = null;
        }
        List<String> tokens = new ArrayList<String>();
        if (csv != null && !csv.trim().isEmpty()) {
            for (String token : csv.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    tokens.add(trimmed);
                }
            }
        }
        return tokens;
    }

    private List<String> entryTokens(SchoolTemplateImportEntry entry) {
        if (entry.getScheduleType() == null) {
            return new ArrayList<String>();
        }
        List<String> tokens;
        if ("WEEKLY".equals(entry.getScheduleType())) {
            tokens = entry.getDaysOfWeek();
        } else if ("MONTHLY".equals(entry.getScheduleType())) {
            tokens = entry.getDaysOfMonth();
        } else if ("QUARTERLY".equals(entry.getScheduleType())) {
            tokens = entry.getDaysOfQuarter();
        } else if ("YEARLY".equals(entry.getScheduleType())) {
            tokens = entry.getDaysOfYear();
        } else {
            tokens = null;
        }
        return tokens == null ? new ArrayList<String>() : tokens;
    }

    private String scheduleLabel(String typeName, List<String> tokens) {
        TemplateType type = TemplateType.valueOf(typeName);
        if (type == TemplateType.DAILY) {
            return "Daily";
        }
        String capitalized = type.getLabel();
        if (tokens == null || tokens.isEmpty()) {
            return capitalized + " (every occurrence)";
        }
        StringBuilder joined = new StringBuilder();
        for (String token : tokens) {
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(token);
        }
        return capitalized + " (" + joined + ")";
    }

    private static boolean eq(String a, String b) {
        return safe(a).equals(safe(b));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        return safe(value).trim().toLowerCase(Locale.ENGLISH);
    }
}
