package org.openimmunizationsoftware.pt.schooltemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses an uploaded Template Scheduler JSON document into {@link SchoolTemplateImportEntry} records.
 * This only checks structure/syntax (field types, allowed enum words, schedule-day token format) — it does
 * NOT check the records against the database (project exists, id exists); that's {@link SchoolTemplateDiffService}'s job,
 * because it needs a live Session to do so.
 * <p>
 * All records are checked and every problem found is collected before failing, so a single re-upload can
 * fix everything ChatGPT got wrong instead of a slow one-error-at-a-time loop.
 */
public class SchoolTemplateImportParser {

    private static final Set<String> ALLOWED_FIELDS = new HashSet<String>(Arrays.asList(
            "id", "delete", "project", "description", "scheduleType", "scheduleDays",
            "missedActionBehavior", "autoGenerate", "actionType", "timeEstimateMinutes", "gamePoints", "timeSlot"));

    private static final Set<String> SCHEDULE_TYPES = new HashSet<String>(
            Arrays.asList("DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"));
    private static final Set<String> MISSED_BEHAVIORS = new HashSet<String>(
            Arrays.asList("AUTO_CANCEL", "CARRY_FORWARD", "IGNORE"));
    private static final Set<String> ACTION_TYPES = new HashSet<String>(
            Arrays.asList("WILL", "MIGHT", "WILL_CONTACT", "COMMITTED_TO"));
    private static final Set<String> TIME_SLOTS = new HashSet<String>(
            Arrays.asList("WAKE", "MORNING", "AFTERNOON", "EVENING"));
    private static final Set<String> DAYS_OF_WEEK = new HashSet<String>(
            Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"));
    private static final Set<String> MONTHS = new HashSet<String>(Arrays.asList(
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"));

    private static final Pattern DAY_OF_MONTH_NUMERIC = Pattern.compile("^([1-9]|[12][0-9]|3[01])$");
    private static final Pattern DAY_OF_QUARTER_NUMERIC = Pattern.compile("^([1-9][0-9]?|9[0-2])$");
    private static final Pattern MMDD = Pattern.compile("^\\d{4}$");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<SchoolTemplateImportEntry> parse(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.length() == 0) {
            throw new IllegalArgumentException("Import data is empty.");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(normalized);
        } catch (IOException e) {
            throw new IllegalArgumentException("Import data is not valid JSON: " + e.getMessage());
        }

        List<JsonNode> templateNodes = collectTemplateNodes(root);
        if (templateNodes.isEmpty()) {
            throw new IllegalArgumentException("Import data does not contain any templates.");
        }

        List<String> errors = new ArrayList<String>();
        List<SchoolTemplateImportEntry> entries = new ArrayList<SchoolTemplateImportEntry>();
        for (int i = 0; i < templateNodes.size(); i++) {
            int recordNumber = i + 1;
            try {
                entries.add(parseEntry(templateNodes.get(i), recordNumber));
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
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

        return entries;
    }

    private List<JsonNode> collectTemplateNodes(JsonNode root) {
        List<JsonNode> nodes = new ArrayList<JsonNode>();
        if (root.isArray()) {
            for (JsonNode item : root) {
                nodes.add(item);
            }
            return nodes;
        }
        if (root.isObject() && root.has("categories") && root.get("categories").isArray()) {
            for (JsonNode category : root.get("categories")) {
                JsonNode templates = category.get("templates");
                if (templates != null && templates.isArray()) {
                    for (JsonNode item : templates) {
                        nodes.add(item);
                    }
                }
            }
            return nodes;
        }
        if (root.isObject() && root.has("templates") && root.get("templates").isArray()) {
            for (JsonNode item : root.get("templates")) {
                nodes.add(item);
            }
            return nodes;
        }
        if (root.isObject()) {
            nodes.add(root);
        }
        return nodes;
    }

    private SchoolTemplateImportEntry parseEntry(JsonNode node, int recordNumber) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Record " + recordNumber + " must be a JSON object.");
        }

        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!ALLOWED_FIELDS.contains(fieldName)) {
                throw new IllegalArgumentException(
                        "Record " + recordNumber + " has unknown field '" + fieldName + "'.");
            }
        }

        SchoolTemplateImportEntry entry = new SchoolTemplateImportEntry(recordNumber);
        entry.setId(readOptionalInt(node, "id", recordNumber));
        entry.setDelete(node.has("delete") && node.get("delete").isBoolean() && node.get("delete").asBoolean());

        if (entry.isDelete()) {
            if (entry.getId() == null) {
                throw new IllegalArgumentException(
                        "Record " + recordNumber + " has \"delete\": true but no \"id\" — deletes must reference an existing template id.");
            }
            return entry;
        }

        entry.setProject(readRequiredText(node, "project", recordNumber, "a project name"));
        entry.setDescription(readRequiredText(node, "description", recordNumber, "a description"));

        String scheduleType = readRequiredText(node, "scheduleType", recordNumber, "a scheduleType").toUpperCase(Locale.ENGLISH);
        if (!SCHEDULE_TYPES.contains(scheduleType)) {
            throw new IllegalArgumentException("Record " + recordNumber + " (" + label(entry) + "): scheduleType '"
                    + scheduleType + "' is not valid. Use one of DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY.");
        }
        entry.setScheduleType(scheduleType);

        JsonNode scheduleDaysNode = node.get("scheduleDays");
        if (scheduleDaysNode != null && scheduleDaysNode.isObject()) {
            entry.setDaysOfWeek(readDayList(scheduleDaysNode, "daysOfWeek", recordNumber, entry, DAYS_OF_WEEK, null));
            entry.setDaysOfMonth(readDayList(scheduleDaysNode, "daysOfMonth", recordNumber, entry, null, "month"));
            entry.setDaysOfQuarter(readDayList(scheduleDaysNode, "daysOfQuarter", recordNumber, entry, null, "quarter"));
            entry.setDaysOfYear(readDayList(scheduleDaysNode, "daysOfYear", recordNumber, entry, null, "year"));
        }

        String missedActionBehavior = readRequiredText(node, "missedActionBehavior", recordNumber,
                "a missedActionBehavior").toUpperCase(Locale.ENGLISH);
        if (!MISSED_BEHAVIORS.contains(missedActionBehavior)) {
            throw new IllegalArgumentException("Record " + recordNumber + " (" + label(entry)
                    + "): missedActionBehavior '" + missedActionBehavior
                    + "' is not valid. Use one of AUTO_CANCEL, CARRY_FORWARD, IGNORE.");
        }
        entry.setMissedActionBehavior(missedActionBehavior);

        JsonNode autoGenerateNode = node.get("autoGenerate");
        if (autoGenerateNode == null || !autoGenerateNode.isBoolean()) {
            throw new IllegalArgumentException(
                    "Record " + recordNumber + " (" + label(entry) + ") requires a true/false \"autoGenerate\".");
        }
        entry.setAutoGenerate(Boolean.valueOf(autoGenerateNode.asBoolean()));

        String actionType = readRequiredText(node, "actionType", recordNumber, "an actionType").toUpperCase(Locale.ENGLISH);
        if (!ACTION_TYPES.contains(actionType)) {
            throw new IllegalArgumentException("Record " + recordNumber + " (" + label(entry) + "): actionType '"
                    + actionType + "' is not valid. Use one of WILL, MIGHT, WILL_CONTACT, COMMITTED_TO.");
        }
        entry.setActionType(actionType);

        entry.setTimeEstimateMinutes(readOptionalNonNegativeInt(node, "timeEstimateMinutes", recordNumber, entry));
        entry.setGamePoints(readOptionalNonNegativeInt(node, "gamePoints", recordNumber, entry));

        JsonNode timeSlotNode = node.get("timeSlot");
        if (timeSlotNode != null && !timeSlotNode.isNull()) {
            if (!timeSlotNode.isTextual()) {
                throw new IllegalArgumentException("Record " + recordNumber + " (" + label(entry) + "): timeSlot must be text.");
            }
            String timeSlot = timeSlotNode.asText().trim().toUpperCase(Locale.ENGLISH);
            if (!TIME_SLOTS.contains(timeSlot)) {
                throw new IllegalArgumentException("Record " + recordNumber + " (" + label(entry) + "): timeSlot '"
                        + timeSlot + "' is not valid. Use one of WAKE, MORNING, AFTERNOON, EVENING.");
            }
            entry.setTimeSlot(timeSlot);
        }

        return entry;
    }

    private String label(SchoolTemplateImportEntry entry) {
        String project = entry.getProject() == null ? "" : entry.getProject();
        if (entry.getId() != null) {
            return "id " + entry.getId() + (project.isEmpty() ? "" : ", " + project);
        }
        return project.isEmpty() ? "new entry" : project;
    }

    private List<String> readDayList(JsonNode scheduleDaysNode, String fieldName, int recordNumber,
            SchoolTemplateImportEntry entry, Set<String> exactTokenSet, String kind) {
        JsonNode listNode = scheduleDaysNode.get(fieldName);
        if (listNode == null || listNode.isNull()) {
            return null;
        }
        if (!listNode.isArray()) {
            throw new IllegalArgumentException(
                    "Record " + recordNumber + " (" + label(entry) + "): scheduleDays." + fieldName + " must be an array of strings.");
        }
        List<String> tokens = new ArrayList<String>();
        for (JsonNode item : listNode) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("Record " + recordNumber + " (" + label(entry)
                        + "): scheduleDays." + fieldName + " must contain only strings.");
            }
            String token = item.asText().trim().toUpperCase(Locale.ENGLISH);
            if (token.isEmpty()) {
                continue;
            }
            if (!isValidDayToken(token, exactTokenSet, kind)) {
                throw new IllegalArgumentException("Record " + recordNumber + " (" + label(entry)
                        + "): scheduleDays." + fieldName + " has an unrecognized value '" + token + "'.");
            }
            tokens.add(token);
        }
        return tokens;
    }

    private boolean isValidDayToken(String token, Set<String> exactTokenSet, String kind) {
        if (exactTokenSet != null) {
            return exactTokenSet.contains(token);
        }
        if ("month".equals(kind)) {
            if (DAY_OF_MONTH_NUMERIC.matcher(token).matches()) {
                return true;
            }
            return isWeekPositionToken(token, 5);
        }
        if ("quarter".equals(kind)) {
            if (DAY_OF_QUARTER_NUMERIC.matcher(token).matches()) {
                return true;
            }
            return isWeekPositionToken(token, 13);
        }
        if ("year".equals(kind)) {
            if (MMDD.matcher(token).matches()) {
                int mm = Integer.parseInt(token.substring(0, 2));
                int dd = Integer.parseInt(token.substring(2, 4));
                return mm >= 1 && mm <= 12 && dd >= 1 && dd <= 31;
            }
            int firstDash = token.indexOf('-');
            if (firstDash < 1) {
                return false;
            }
            String monthPart = token.substring(0, firstDash);
            String weekDayPart = token.substring(firstDash + 1);
            return MONTHS.contains(monthPart) && isWeekPositionToken(weekDayPart, 5);
        }
        return false;
    }

    private boolean isWeekPositionToken(String token, int maxWeek) {
        if (!(token.startsWith("W"))) {
            return false;
        }
        int dashIdx = token.indexOf('-');
        if (dashIdx < 2) {
            return false;
        }
        String weekPart = token.substring(1, dashIdx);
        String dayPart = token.substring(dashIdx + 1);
        if (!DAYS_OF_WEEK.contains(dayPart)) {
            return false;
        }
        if ("L".equals(weekPart)) {
            return true;
        }
        try {
            int weekNum = Integer.parseInt(weekPart);
            return weekNum >= 1 && weekNum <= maxWeek;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Integer readOptionalInt(JsonNode node, String fieldName, int recordNumber) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException("Record " + recordNumber + " field '" + fieldName + "' must be a whole number.");
        }
        return Integer.valueOf(value.asInt());
    }

    private Integer readOptionalNonNegativeInt(JsonNode node, String fieldName, int recordNumber,
            SchoolTemplateImportEntry entry) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || value.asInt() < 0) {
            throw new IllegalArgumentException(
                    "Record " + recordNumber + " (" + label(entry) + "): '" + fieldName + "' must be a non-negative whole number.");
        }
        return Integer.valueOf(value.asInt());
    }

    private String readRequiredText(JsonNode node, String fieldName, int recordNumber, String description) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().trim().length() == 0) {
            throw new IllegalArgumentException("Record " + recordNumber + " requires " + description + ".");
        }
        return value.asText().trim();
    }
}
