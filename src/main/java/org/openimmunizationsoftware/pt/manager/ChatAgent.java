package org.openimmunizationsoftware.pt.manager;

import java.io.IOException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatAgent {

    public static final String RESPONSE_FORMAT_TEXT = "text";
    public static final String RESPONSE_FORMAT_JSON = "json_object";

    private String title = "";
    private String rawRequestBody = null;
    private String rawResponseBody = null;
    private String responseText = null;
    private String responseError = null;
    private String responseFormat = RESPONSE_FORMAT_TEXT;
    private String requestPrompt = null;

    public String getTitle() {
        return title;
    }

    public String getRequestPrompt() {
        return requestPrompt;
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public boolean hasResponse() {
        return responseText != null && responseError == null;
    }

    public String getRawRequestBody() {
        return rawRequestBody;
    }

    public String getRawResponseBody() {
        return rawResponseBody;
    }

    public String getResponseText() {
        return responseText;
    }

    public String getResponseError() {
        return responseError;
    }

    private String systemInsructions = "You are a helpful assistant tasked with supporting a public health professional organize their work projects, actions taken, and next steps.";

    public String getSystemInstructions() {
        return systemInsructions;
    }

    public void setSystemInstructions(String systemInsructions) {
        this.systemInsructions = systemInsructions;
    }

    public ChatAgent() {
    }

    public ChatAgent(String title, String systemInsructions) {
        this.title = title;
        this.systemInsructions = systemInsructions;
    }

    public void chat(String prompt) {
        this.requestPrompt = prompt;
        String endpoint = "https://api.openai.com/v1/chat/completions";
        ObjectMapper objectMapper = new ObjectMapper();

        String apiKey = System.getenv("CHATGPT_API_KEY_TOMCAT");

        // Request parameters
        String modelId = "gpt-4o";

        // Create an HTTP client
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Create a POST request
            HttpPost postRequest = new HttpPost(endpoint);
            postRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            postRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            // Create the request body
            rawRequestBody = "{\"model\": \""
                    + modelId
                    + "\", \"messages\": [{\"role\": \"system\", \"content\": [{ \"type\": \"text\", \"text\":  "
                    + objectMapper.writeValueAsString(systemInsructions) + "}]}, "
                    + "{\"role\": \"user\", \"content\": [{ \"type\": \"text\", \"text\":  "
                    + objectMapper.writeValueAsString(prompt) + "}]}], "
                    + "\"temperature\": 1, \"max_tokens\": 2048, \"top_p\": 1, \"frequency_penalty\": 0, \"presence_penalty\": 0, "
                    + "\"response_format\": {\"type\": \"" + responseFormat + "\"}}";

            postRequest.setEntity(new StringEntity(rawRequestBody));

            // Send the request and get the response
            HttpResponse postResponse = httpClient.execute(postRequest);

            // Process the response
            rawResponseBody = EntityUtils.toString(postResponse.getEntity());

            JsonNode rootNode = objectMapper.readTree(rawResponseBody);
            if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
                // Extract the "content" field from the first choice
                JsonNode firstChoice = rootNode.get("choices").get(0);
                JsonNode messageNode = firstChoice.get("message");
                if (messageNode != null && messageNode.has("content")) {
                    String returnedText = messageNode.get("content").asText();
                    responseText = returnedText;
                } else {
                    responseError = "Unable to find the \"content\" field";
                }
            } else {
                responseError = "Unable to find the \"choices\" field";
            }
        } catch (IOException ex) {
            responseError = ex.getMessage();
        }

    }
}
