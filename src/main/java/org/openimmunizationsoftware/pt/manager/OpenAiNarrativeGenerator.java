package org.openimmunizationsoftware.pt.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class OpenAiNarrativeGenerator implements NarrativeGenerator {

    private static final ChatModel MODEL = ChatModel.GPT_5_2;

    private static final String SYSTEM_PROMPT = "You are generating an operational daily summary report for a personal project tracker.\n"
            + "Output MUST be GitHub-flavored Markdown only.\n"
            + "Use only: headings, bold, italics, bullet lists, and short paragraphs. No tables unless the user data already implies a table. No code fences.\n\n"
            + "Goals:\n"
            + "- Accurately summarize what happened today based ONLY on the provided data.\n"
            + "- Be concise and concrete. Prefer verbs and outcomes.\n"
            + "- Do not invent facts. If data is missing, omit it.\n"
            + "- Do not add life advice, strategy, or long reflection.\n\n"
            + "Required structure:\n"
            + "# Daily Summary — {DATE}\n"
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
        String apiKey = System.getenv("CHATGPT_API_KEY_TOMCAT");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("Missing CHATGPT_API_KEY_TOMCAT environment variable for OpenAI API.");
        }
        this.client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    }

    @Override
    public String generateDailyMarkdown(GenerationContext ctx) {
        String input = buildDailyInput(ctx);
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
            throw new RuntimeException(message, e);
        }
    }

    private String buildDailyInput(GenerationContext ctx) {
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

        sb.append("Completed Actions\n");
        Map<String, List<String>> completedByProject = new LinkedHashMap<String, List<String>>();
        for (ProjectActionTaken action : ctx.getCompletedActions()) {
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
            for (ProjectActionNext action : ctx.getWaitingActions()) {
                String projectName = action.getProject() == null ? "Unassigned" : action.getProject().getProjectName();
                sb.append("- ").append(projectName).append(": ").append(action.getNextDescription()).append("\n");
            }
        }
        return sb.toString();
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
        Object directText = tryInvoke(item, "text");
        if (directText != null) {
            sb.append(String.valueOf(directText));
            return;
        }
        Object content = tryInvoke(item, "content");
        if (content instanceof Iterable) {
            for (Object part : (Iterable<?>) content) {
                Object partText = tryInvoke(part, "text");
                if (partText != null) {
                    sb.append(String.valueOf(partText));
                }
            }
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
}
