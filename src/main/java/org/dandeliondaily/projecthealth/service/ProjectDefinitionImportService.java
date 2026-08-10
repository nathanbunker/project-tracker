package org.dandeliondaily.projecthealth.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProjectDefinitionImportService {

    private static final Set<String> ALLOWED_FIELDS = new HashSet<String>();

    static {
        ALLOWED_FIELDS.add("projectName");
        ALLOWED_FIELDS.add("description");
        ALLOWED_FIELDS.add("currentFocus");
        ALLOWED_FIELDS.add("projectOutcome");
        ALLOWED_FIELDS.add("successCriteria");
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public static class ProjectDefinitionPatch {
        private String projectName;
        private boolean descriptionPresent;
        private String description;
        private boolean currentFocusPresent;
        private String currentFocus;
        private boolean projectOutcomePresent;
        private String projectOutcome;
        private boolean successCriteriaPresent;
        private String successCriteria;

        public String getProjectName() {
            return projectName;
        }

        public boolean isDescriptionPresent() {
            return descriptionPresent;
        }

        public String getDescription() {
            return description;
        }

        public boolean isCurrentFocusPresent() {
            return currentFocusPresent;
        }

        public String getCurrentFocus() {
            return currentFocus;
        }

        public boolean isProjectOutcomePresent() {
            return projectOutcomePresent;
        }

        public String getProjectOutcome() {
            return projectOutcome;
        }

        public boolean isSuccessCriteriaPresent() {
            return successCriteriaPresent;
        }

        public String getSuccessCriteria() {
            return successCriteria;
        }
    }

    public List<ProjectDefinitionPatch> parse(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.length() == 0) {
            throw new IllegalArgumentException("Import data is empty.");
        }

        List<JsonNode> records = parseRecords(normalized);
        List<ProjectDefinitionPatch> patches = new ArrayList<ProjectDefinitionPatch>();
        for (int i = 0; i < records.size(); i++) {
            patches.add(parsePatch(records.get(i), i + 1));
        }
        if (patches.isEmpty()) {
            throw new IllegalArgumentException("Import data does not contain any projects.");
        }
        return patches;
    }

    private List<JsonNode> parseRecords(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            List<JsonNode> records = new ArrayList<JsonNode>();
            if (root.isArray()) {
                for (JsonNode item : root) {
                    records.add(item);
                }
            } else {
                records.add(root);
            }
            return records;
        } catch (IOException wholeDocumentError) {
            return parseJsonLines(content);
        }
    }

    private List<JsonNode> parseJsonLines(String content) {
        List<JsonNode> records = new ArrayList<JsonNode>();
        String[] lines = content.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            try {
                records.add(objectMapper.readTree(line));
            } catch (IOException e) {
                throw new IllegalArgumentException("Line " + (i + 1) + " is not valid JSON.");
            }
        }
        return records;
    }

    private ProjectDefinitionPatch parsePatch(JsonNode node, int recordNumber) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Record " + recordNumber + " must be a JSON object.");
        }
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!ALLOWED_FIELDS.contains(fieldName)) {
                throw new IllegalArgumentException("Record " + recordNumber + " has unknown field '"
                        + fieldName + "'.");
            }
        }

        ProjectDefinitionPatch patch = new ProjectDefinitionPatch();
        patch.projectName = readRequiredText(node, "projectName", recordNumber).trim();
        patch.descriptionPresent = node.has("description");
        patch.description = readOptionalText(node, "description", recordNumber);
        patch.currentFocusPresent = node.has("currentFocus");
        patch.currentFocus = readOptionalText(node, "currentFocus", recordNumber);
        patch.projectOutcomePresent = node.has("projectOutcome");
        patch.projectOutcome = readOptionalText(node, "projectOutcome", recordNumber);
        patch.successCriteriaPresent = node.has("successCriteria");
        patch.successCriteria = readSuccessCriteria(node.get("successCriteria"), recordNumber);

        if (!patch.descriptionPresent && !patch.currentFocusPresent && !patch.projectOutcomePresent
                && !patch.successCriteriaPresent) {
            throw new IllegalArgumentException("Record " + recordNumber + " does not contain any fields to update.");
        }
        return patch;
    }

    private String readRequiredText(JsonNode node, String fieldName, int recordNumber) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().trim().length() == 0) {
            throw new IllegalArgumentException("Record " + recordNumber + " requires a non-empty projectName.");
        }
        return value.asText();
    }

    private String readOptionalText(JsonNode node, String fieldName, int recordNumber) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException("Record " + recordNumber + " field '" + fieldName
                    + "' must be text or null.");
        }
        return value.asText();
    }

    private String readSuccessCriteria(JsonNode value, int recordNumber) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return value.asText();
        }
        if (!value.isArray()) {
            throw new IllegalArgumentException("Record " + recordNumber
                    + " field 'successCriteria' must be an array of text values, text, or null.");
        }
        StringBuilder joined = new StringBuilder();
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("Record " + recordNumber
                        + " field 'successCriteria' must contain only text values.");
            }
            if (joined.length() > 0) {
                joined.append('\n');
            }
            joined.append(item.asText());
        }
        return joined.toString();
    }
}
