package org.openimmunizationsoftware.pt.manager;

import java.util.ArrayList;
import java.util.List;

import org.dandeliondaily.dashboard.model.ProjectDashboardChatMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class ProjectReviewChatService {

    private static final ChatModel MODEL = ChatModel.GPT_5_2;
    private static final int MAX_HISTORY_MESSAGES = 24;

    private static final String SYSTEM_PROMPT = "You are a project review assistant for Dandelion Daily. "
            + "You help improve project definition only. Do not suggest direct autonomous edits. "
            + "Return JSON only with keys: assistantMessage, proposedDescription, proposedOutcome, proposedSuccessCriteria, followUpQuestions. "
            + "assistantMessage should be concise and practical. followUpQuestions should be an array of strings when useful.";

    private final OpenAIClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProjectReviewChatService() {
        String apiKey = readApiKey();
        this.client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    }

    public static boolean isConfigured() {
        String apiKey = System.getenv(OpenAiNarrativeGenerator.API_KEY_ENV);
        return apiKey != null && apiKey.trim().length() > 0;
    }

    public static String getMissingConfigurationMessage() {
        return "Project chat is not available because the OpenAI API key is not configured. Set "
                + OpenAiNarrativeGenerator.API_KEY_ENV + " for Tomcat and restart.";
    }

    public ProjectReviewChatResponse chat(List<ProjectDashboardChatMessage> history, String userPrompt,
            String contextText) {
        String input = buildInput(history, userPrompt, contextText);
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(MODEL)
                .instructions(SYSTEM_PROMPT)
                .input(input)
                .build();
        Response response = client.responses().create(params);
        String rawText = extractText(response);
        return parseResponse(rawText);
    }

    private String buildInput(List<ProjectDashboardChatMessage> history, String userPrompt, String contextText) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Context\n");
        sb.append(contextText).append("\n\n");
        sb.append("Conversation History\n");
        List<ProjectDashboardChatMessage> trimmed = trimHistory(history);
        if (trimmed.isEmpty()) {
            sb.append("- (none)\n");
        } else {
            for (ProjectDashboardChatMessage message : trimmed) {
                sb.append("- ").append(message.getRole()).append(": ").append(n(message.getText())).append("\n");
            }
        }
        sb.append("\nUser Prompt\n");
        sb.append(userPrompt);
        return sb.toString();
    }

    private List<ProjectDashboardChatMessage> trimHistory(List<ProjectDashboardChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<ProjectDashboardChatMessage>();
        }
        if (history.size() <= MAX_HISTORY_MESSAGES) {
            return history;
        }
        return history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size());
    }

    private ProjectReviewChatResponse parseResponse(String rawText) {
        ProjectReviewChatResponse parsed = new ProjectReviewChatResponse();
        if (rawText == null || rawText.trim().length() == 0) {
            parsed.setAssistantMessage("I couldn't generate a response. Please try again.");
            return parsed;
        }

        String cleaned = rawText.trim();
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            int firstBreak = cleaned.indexOf('\n');
            if (firstBreak > -1) {
                cleaned = cleaned.substring(firstBreak + 1);
            }
            int lastFence = cleaned.lastIndexOf("```");
            if (lastFence > -1) {
                cleaned = cleaned.substring(0, lastFence);
            }
            cleaned = cleaned.trim();
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);
            parsed.setAssistantMessage(readText(root, "assistantMessage"));
            parsed.setProposedDescription(readText(root, "proposedDescription"));
            parsed.setProposedOutcome(readText(root, "proposedOutcome"));
            parsed.setProposedSuccessCriteria(readText(root, "proposedSuccessCriteria"));
            JsonNode followUpNode = root.get("followUpQuestions");
            if (followUpNode != null && followUpNode.isArray()) {
                List<String> followUps = new ArrayList<String>();
                for (JsonNode item : followUpNode) {
                    if (item != null && item.asText().trim().length() > 0) {
                        followUps.add(item.asText().trim());
                    }
                }
                parsed.setFollowUpQuestions(followUps);
            }
            if (parsed.getAssistantMessage() == null || parsed.getAssistantMessage().trim().length() == 0) {
                parsed.setAssistantMessage(cleaned);
            }
            return parsed;
        } catch (Exception parseError) {
            parsed.setAssistantMessage(cleaned);
            return parsed;
        }
    }

    private String readText(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private String extractText(Response response) {
        if (response == null || response.output() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object item : response.output()) {
            appendTextFromItem(sb, item);
        }
        return sb.toString().trim();
    }

    private void appendTextFromItem(StringBuilder sb, Object item) {
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

    private void appendFromContent(StringBuilder sb, Object container) {
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

    private void appendTextValue(StringBuilder sb, Object value) {
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

    private Object tryInvokeAny(Object target, String... methodNames) {
        if (target == null || methodNames == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Object value = target.getClass().getMethod(methodName).invoke(target);
                value = unwrapOptional(value);
                if (value != null) {
                    return value;
                }
            } catch (Exception ignored) {
                // try next method
            }
        }
        return null;
    }

    private Object tryGetFieldAny(Object target, String... fieldNames) {
        if (target == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            try {
                java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                value = unwrapOptional(value);
                if (value != null) {
                    return value;
                }
            } catch (Exception ignored) {
                // try next field
            }
        }
        return null;
    }

    private Object unwrapOptional(Object value) {
        if (value instanceof java.util.Optional) {
            java.util.Optional<?> optional = (java.util.Optional<?>) value;
            return optional.isPresent() ? optional.get() : null;
        }
        return value;
    }

    private String readApiKey() {
        String apiKey = System.getenv(OpenAiNarrativeGenerator.API_KEY_ENV);
        if (apiKey == null || apiKey.trim().length() == 0) {
            throw new IllegalStateException(getMissingConfigurationMessage());
        }
        return apiKey.trim();
    }

    private String n(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
