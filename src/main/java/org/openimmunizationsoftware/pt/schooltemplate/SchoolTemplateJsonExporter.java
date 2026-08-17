package org.openimmunizationsoftware.pt.schooltemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionNextTemplateConfig;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.TimeSlot;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Builds the round-trippable JSON representation of a dependent's Template Scheduler (School/Chores) list. */
public class SchoolTemplateJsonExporter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String toJson(int dependencyId, String dependentName, List<SchoolTemplateSnapshot> snapshots)
            throws Exception {
        Map<String, Object> root = new LinkedHashMap<String, Object>();

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        isoFormat.setTimeZone(TimeZone.getDefault());
        root.put("exportedAt", isoFormat.format(new Date()));
        root.put("dependencyId", Integer.valueOf(dependencyId));
        root.put("dependentName", dependentName == null ? "" : dependentName);

        List<Map<String, Object>> categories = new ArrayList<Map<String, Object>>();
        categories.add(buildCategory("School", snapshots, true));
        categories.add(buildCategory("Chores", snapshots, false));
        root.put("categories", categories);

        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private Map<String, Object> buildCategory(String categoryLabel, List<SchoolTemplateSnapshot> snapshots,
            boolean billable) {
        List<SchoolTemplateSnapshot> filtered = new ArrayList<SchoolTemplateSnapshot>();
        for (SchoolTemplateSnapshot snapshot : snapshots) {
            if (snapshot.isBillable() == billable) {
                filtered.add(snapshot);
            }
        }
        Collections.sort(filtered, new Comparator<SchoolTemplateSnapshot>() {
            @Override
            public int compare(SchoolTemplateSnapshot left, SchoolTemplateSnapshot right) {
                String leftName = left.getProject() == null ? "" : left.getProject().getProjectName();
                String rightName = right.getProject() == null ? "" : right.getProject().getProjectName();
                int nameCmp = safe(leftName).compareToIgnoreCase(safe(rightName));
                if (nameCmp != 0) {
                    return nameCmp;
                }
                return safe(left.getActionNext().getNextDescription())
                        .compareToIgnoreCase(safe(right.getActionNext().getNextDescription()));
            }
        });

        Map<String, Object> categoryMap = new LinkedHashMap<String, Object>();
        categoryMap.put("category", categoryLabel);
        List<Map<String, Object>> templates = new ArrayList<Map<String, Object>>();
        for (SchoolTemplateSnapshot snapshot : filtered) {
            templates.add(buildTemplateEntry(snapshot));
        }
        categoryMap.put("templates", templates);
        return categoryMap;
    }

    private Map<String, Object> buildTemplateEntry(SchoolTemplateSnapshot snapshot) {
        ActionNext actionNext = snapshot.getActionNext();
        ActionNextTemplateConfig config = snapshot.getConfig();

        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("id", Integer.valueOf(actionNext.getActionNextId()));
        entry.put("project", snapshot.getProject() == null ? "" : safe(snapshot.getProject().getProjectName()));
        entry.put("description", safe(actionNext.getNextDescription()));

        TemplateType templateType = actionNext.getTemplateType() != null ? actionNext.getTemplateType()
                : TemplateType.DAILY;
        entry.put("scheduleType", templateType.name());

        Map<String, Object> scheduleDays = new LinkedHashMap<String, Object>();
        if (config != null) {
            switch (templateType) {
                case WEEKLY:
                    scheduleDays.put("daysOfWeek", splitCsv(config.getScheduleDaysOfWeek()));
                    break;
                case MONTHLY:
                    scheduleDays.put("daysOfMonth", splitCsv(config.getScheduleDaysOfMonth()));
                    break;
                case QUARTERLY:
                    scheduleDays.put("daysOfQuarter", splitCsv(config.getScheduleDaysOfQuarter()));
                    break;
                case YEARLY:
                    scheduleDays.put("daysOfYear", splitCsv(config.getScheduleDaysOfYear()));
                    break;
                default:
                    break;
            }
        }
        entry.put("scheduleDays", scheduleDays);

        String missedActionBehavior = config != null ? safe(config.getMissedActionBehavior()) : "AUTO_CANCEL";
        entry.put("missedActionBehavior", missedActionBehavior.isEmpty() ? "AUTO_CANCEL" : missedActionBehavior);
        entry.put("autoGenerate", Boolean.valueOf(config == null || config.isAutoGenerate()));

        String actionType = safe(actionNext.getNextActionType());
        entry.put("actionType", actionType.isEmpty() ? "WILL" : actionType);

        if (snapshot.isBillable()) {
            entry.put("timeEstimateMinutes",
                    Integer.valueOf(actionNext.getNextTimeEstimate() == null ? 0 : actionNext.getNextTimeEstimate()));
            entry.put("gamePoints",
                    Integer.valueOf(actionNext.getGamePoints() == null ? 0 : actionNext.getGamePoints()));
        } else {
            TimeSlot timeSlot = actionNext.getTimeSlot() != null ? actionNext.getTimeSlot() : TimeSlot.AFTERNOON;
            entry.put("timeSlot", timeSlot.name());
        }

        return entry;
    }

    private static List<String> splitCsv(String value) {
        List<String> tokens = new ArrayList<String>();
        if (value == null || value.trim().isEmpty()) {
            return tokens;
        }
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
