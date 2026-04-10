package org.openimmunizationsoftware.pt.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.Project;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class OpenAiNarrativeGenerator implements NarrativeGenerator {

    public static final String API_KEY_ENV = "CHATGPT_API_KEY_TOMCAT";

    private static final ChatModel MODEL = ChatModel.GPT_5_2;

    private static final String SYSTEM_PROMPT = "You are generating an operational daily summary report for a private Dandelion workspace.\n"
            + "Output MUST be GitHub-flavored Markdown only.\n"
            + "Use only: headings, bold, italics, bullet lists, and short paragraphs. No tables unless the user data already implies a table. No code fences.\n\n"
            + "Goals:\n"
            + "- Accurately summarize what happened today based ONLY on the provided data.\n"
            + "- Be concise and concrete. Prefer verbs and outcomes.\n"
            + "- Do not invent facts. If data is missing, omit it.\n"
            + "- Do not add life advice, strategy, or long reflection.\n\n"
            + "Required structure:\n"
            + "# Daily Summary - {DATE}\n"
            + "## Time Overview\n"
            + "- Total tracked time: ...\n"
            + "- Top projects by time: ...\n"
            + "## Completed Work\n"
            + "- Group by project: Project Name (time) then bullets of completed actions with short outcome phrases.\n"
            + "## Key Notes\n"
            + "- Group by project; include only the most important NOTE entries.\n"
            + "## Decisions\n"
            + "- Bullets; include project name prefix.\n"
            + "## Insights\n"
            + "- Bullets; include project name prefix.\n"
            + "## Risks\n"
            + "- Bullets; include project name prefix; keep as risk statements.\n"
            + "## Opportunities\n"
            + "- Bullets; include project name prefix.\n\n"
            + "If there are no items in a section, omit the section.";

    private final OpenAIClient client;

    public OpenAiNarrativeGenerator() {
        String apiKey = readApiKey();
        this.client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    }

    public static boolean isConfigured() {
        return readConfiguredApiKey() != null;
    }

    public static String getMissingConfigurationMessage() {
        return "Tracker narrative generation is not available because the OpenAI API key is not configured. "
                + "Set " + API_KEY_ENV + " for the Tomcat process and restart Tomcat.";
    }

    @Override
    public String generateDailyMarkdown(GenerationContext ctx) {
        String input = buildDailyInputText(ctx);
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(MODEL)
                .instructions(SYSTEM_PROMPT)
                .input(input)
                .build();
        try {
            Response response = client.responses().create(params);
            String markdown = extractMarkdown(response);
            return sanitizeMarkdown(markdown);
        } catch (Exception e) {
            String status = tryExtractStatusCode(e);
            String requestId = tryExtractRequestId(e);
            StringBuilder detail = new StringBuilder();
            if (status != null) {
                detail.append(" Status=").append(status);
            }
            if (requestId != null) {
                detail.append(" RequestId=").append(requestId);
            }
            String message = "OpenAI narrative generation failed." + detail.toString();
            if ("401".equals(status)) {
                message = "OpenAI authentication failed (401)." + detail.toString();
            } else if ("429".equals(status)) {
                message = "OpenAI rate limited (429)." + detail.toString();
            } else if (status != null && status.startsWith("5")) {
                message = "OpenAI server error (" + status + ")." + detail.toString();
            }
            System.out.println("[OpenAI] " + message);
            throw new RuntimeException(message, e);
        }
    }

    public static String buildPromptForInspection(GenerationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SYSTEM INSTRUCTIONS ===\n");
        sb.append(SYSTEM_PROMPT).append("\n\n");
        sb.append("=== INPUT PAYLOAD ===\n");
        sb.append(buildDailyInputText(ctx));
        return sb.toString();
    }

    private static String buildDailyInputText(GenerationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(ctx.getPeriodStart()).append(" to ").append(ctx.getPeriodEnd()).append("\n");
        sb.append("\nTime Summary\n");
        int totalMinutes = 0;
        List<Map.Entry<Integer, Integer>> timeEntries = new ArrayList<Map.Entry<Integer, Integer>>(
                ctx.getTimeByProject().entrySet());
        timeEntries.sort(new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> left, Map.Entry<Integer, Integer> right) {
                return right.getValue().compareTo(left.getValue());
            }
        });
        for (Map.Entry<Integer, Integer> entry : timeEntries) {
            totalMinutes += entry.getValue() == null ? 0 : entry.getValue();
            String name = ctx.getProjectNames().get(entry.getKey());
            sb.append("- ").append(name == null ? "Project " + entry.getKey() : name)
                    .append(": ").append(TimeTracker.formatTime(entry.getValue())).append("\n");
        }
        sb.append("Total: ").append(TimeTracker.formatTime(totalMinutes)).append("\n\n");

        appendProjectContext(sb, ctx, timeEntries);

        sb.append("Completed Actions\n");
        Map<String, List<String>> completedByProject = new LinkedHashMap<String, List<String>>();
        for (ActionTaken action : ctx.getCompletedActions()) {
            String projectName = action.getProject() == null ? "Unassigned" : action.getProject().getProjectName();
            List<String> actions = completedByProject.get(projectName);
            if (actions == null) {
                actions = new ArrayList<String>();
                completedByProject.put(projectName, actions);
            }
            actions.add(action.getActionDescription());
        }
        appendProjectList(sb, completedByProject);

        sb.append("\nProject Narratives\n");
        appendNarrativeGroup(sb, "NOTE", ProjectNarrativeVerb.NOTE, ctx);
        appendNarrativeGroup(sb, "DECISION", ProjectNarrativeVerb.DECISION, ctx);
        appendNarrativeGroup(sb, "INSIGHT", ProjectNarrativeVerb.INSIGHT, ctx);
        appendNarrativeGroup(sb, "RISK", ProjectNarrativeVerb.RISK, ctx);
        appendNarrativeGroup(sb, "OPPORTUNITY", ProjectNarrativeVerb.OPPORTUNITY, ctx);

        sb.append("\nWaiting / Blocked\n");
        if (ctx.getWaitingActions().isEmpty()) {
            sb.append("- None\n");
        } else {
            for (ActionNext action : ctx.getWaitingActions()) {
                String projectName = action.getProject() == null ? "Unassigned" : action.getProject().getProjectName();
                sb.append("- ").append(projectName).append(": ").append(action.getNextDescription()).append("\n");
            }
        }
        return sb.toString();
    }

    private static void appendProjectContext(StringBuilder sb, GenerationContext ctx,
            List<Map.Entry<Integer, Integer>> orderedTimeEntries) {
        sb.append("Project Context\n");
        boolean addedAny = false;

        for (Map.Entry<Integer, Integer> entry : orderedTimeEntries) {
            Integer projectId = entry.getKey();
            String projectName = ctx.getProjectNames().get(projectId);
            Project project = ctx.getProjectsById().get(projectId);
            List<String> openIssues = ctx.getOpenIssuesByProject().get(projectId);

            String description = project == null ? null : project.getDescription();
            String outcomeText = project == null ? null : project.getOutcomeText();
            String successCriteriaText = project == null ? null : project.getSuccessCriteriaText();

            List<String> successCriteriaLines = splitNonEmptyLines(successCriteriaText);

            boolean hasDescription = !isEmpty(description);
            boolean hasOutcome = !isEmpty(outcomeText);
            boolean hasSuccessCriteria = !successCriteriaLines.isEmpty();
            boolean hasOpenIssues = openIssues != null && !openIssues.isEmpty();

            if (!hasDescription && !hasOutcome && !hasSuccessCriteria && !hasOpenIssues) {
                continue;
            }

            addedAny = true;
            sb.append("Project: ").append(projectName == null ? "Project " + projectId : projectName).append("\n");

            if (hasDescription) {
                sb.append("Project Description\n");
                sb.append(description.trim()).append("\n");
            }
            if (hasOutcome) {
                sb.append("Project Outcome\n");
                sb.append(outcomeText.trim()).append("\n");
            }
            if (hasSuccessCriteria) {
                sb.append("Project Success Criteria\n");
                for (String line : successCriteriaLines) {
                    sb.append("- ").append(line).append("\n");
                }
            }
            if (hasOpenIssues) {
                sb.append("Open Issues\n");
                for (String issue : openIssues) {
                    if (isEmpty(issue)) {
                        continue;
                    }
                    sb.append("- ").append(issue.trim()).append("\n");
                }
            }

            sb.append("\n");
        }

        if (!addedAny) {
            sb.append("- None\n\n");
        }
    }

    private static void appendProjectList(StringBuilder sb, Map<String, List<String>> grouped) {
        if (grouped.isEmpty()) {
            sb.append("- None\n");
            return;
        }
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            sb.append("- ").append(entry.getKey()).append("\n");
            for (String item : entry.getValue()) {
                sb.append("  - ").append(item).append("\n");
            }
        }
    }

    private static List<String> splitNonEmptyLines(String value) {
        List<String> lines = new ArrayList<String>();
        if (isEmpty(value)) {
            return lines;
        }
        String[] parts = value.split("\\r?\\n");
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.length() > 0) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static void appendNarrativeGroup(StringBuilder sb, String label, ProjectNarrativeVerb verb,
            GenerationContext ctx) {
        sb.append(label).append("\n");
        Map<String, List<String>> grouped = new LinkedHashMap<String, List<String>>();
        for (ProjectNarrative narrative : ctx.getProjectNarratives()) {
            if (narrative.getNarrativeVerb() != verb) {
                continue;
            }
            String projectName = narrative.getProject() == null ? "Unassigned"
                    : narrative.getProject().getProjectName();
            List<String> items = grouped.get(projectName);
            if (items == null) {
                items = new ArrayList<String>();
                grouped.put(projectName, items);
            }
            items.add(narrative.getNarrativeText());
        }
        appendProjectList(sb, grouped);
        sb.append("\n");
    }

    private static String extractMarkdown(Response response) {
        if (response == null) {
            return "";
        }
        if (response.output() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object item : response.output()) {
            appendTextFromItem(sb, item);
        }
        return sb.toString().trim();
    }

    private static String sanitizeMarkdown(String markdown) {
        if (markdown == null) {
            return "";
        }
        String cleaned = markdown.trim();
        cleaned = cleaned.replace("\u2014", " - ")
                .replace("\u2013", "-")
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u00A0", " ");
        if (cleaned.startsWith("```")) {
            int firstBreak = cleaned.indexOf('\n');
            if (firstBreak > -1) {
                cleaned = cleaned.substring(firstBreak + 1);
            }
            int lastFence = cleaned.lastIndexOf("```");
            if (lastFence > -1) {
                cleaned = cleaned.substring(0, lastFence);
            }
        }
        return cleaned.trim();
    }

    private static String readApiKey() {
        String apiKey = readConfiguredApiKey();
        if (apiKey == null) {
            throw new IllegalStateException(getMissingConfigurationMessage());
        }
        return apiKey;
    }

    private static String readConfiguredApiKey() {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null) {
            return null;
        }
        apiKey = apiKey.trim();
        return apiKey.length() == 0 ? null : apiKey;
    }

    private static String tryExtractStatusCode(Exception e) {
        Object value = tryInvoke(e, "statusCode");
        return value == null ? null : String.valueOf(value);
    }

    private static String tryExtractRequestId(Exception e) {
        Object value = tryInvoke(e, "requestId");
        return value == null ? null : String.valueOf(value);
    }

    private static Object tryInvoke(Exception e, String methodName) {
        try {
            return e.getClass().getMethod(methodName).invoke(e);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void appendTextFromItem(StringBuilder sb, Object item) {
        if (item == null) {
            return;
        }
        Object message = tryInvokeAny(item, "message", "getMessage");
        if (message == null) {
            message = tryGetFieldAny(item, "message");
        }
        if (message != null) {
            appendFromContent(sb, message);
            return;
        }
        appendFromContent(sb, item);
    }

    private static void appendFromContent(StringBuilder sb, Object container) {
        Object content = tryInvokeAny(container, "content", "getContent");
        if (content == null) {
            content = tryGetFieldAny(container, "content");
        }
        if (content instanceof Iterable) {
            for (Object part : (Iterable<?>) content) {
                Object outputText = tryInvokeAny(part, "outputText", "getOutputText");
                if (outputText == null) {
                    outputText = tryGetFieldAny(part, "outputText");
                }
                if (outputText != null) {
                    appendTextValue(sb, outputText);
                    continue;
                }
                Object text = tryInvokeAny(part, "text", "getText");
                if (text == null) {
                    text = tryGetFieldAny(part, "text");
                }
                if (text != null) {
                    sb.append(String.valueOf(text));
                }
            }
        }
    }

    private static void appendTextValue(StringBuilder sb, Object value) {
        Object text = tryInvokeAny(value, "text", "getText");
        if (text == null) {
            text = tryGetFieldAny(value, "text");
        }
        if (text != null) {
            sb.append(String.valueOf(text));
        } else {
            sb.append(String.valueOf(value));
        }
    }

    private static Object tryInvoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object tryInvokeAny(Object target, String... methodNames) {
        if (methodNames == null) {
            return null;
        }
        for (String name : methodNames) {
            Object value = tryInvoke(target, name);
            value = unwrapOptional(value);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Object tryGetFieldAny(Object target, String... fieldNames) {
        if (target == null || fieldNames == null) {
            return null;
        }
        for (String name : fieldNames) {
            try {
                java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(target);
                value = unwrapOptional(value);
                if (value != null) {
                    return value;
                }
            } catch (Exception ignored) {
                // ignore and continue
            }
        }
        return null;
    }

    private static Object unwrapOptional(Object value) {
        if (value instanceof java.util.Optional) {
            java.util.Optional<?> optional = (java.util.Optional<?>) value;
            return optional.isPresent() ? optional.get() : null;
        }
        return value;
    }
}
