package com.aiops.client;

import com.aiops.context.AiJobContext;
import com.aiops.properties.PythonServiceProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PythonAiClient {

    private final RestClient restClient;
    private final PythonServiceProperties properties;
    private final ObjectMapper objectMapper;

    public String getBaseUrl() {
        return properties.getBaseUrl();
    }

    public RestClient getRestClient() {
        return restClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateReport(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/ai/report")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateContent(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/ai/content")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateNegativeReply(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/ai/negative-reply")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> translateComment(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/ai/comment-translate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateProductCompare(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/ai/product-compare")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRagStatus() {
        return restClient.get()
                .uri(properties.getBaseUrl() + "/internal/ai/rag/status")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> reindexRag() {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/ai/rag/reindex")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }

    private String jsonBody(Map<String, Object> request) {
        try {
            return objectMapper.writeValueAsString(withJobMetadata(request));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize Python AI request", exception);
        }
    }

    private Map<String, Object> withJobMetadata(Map<String, Object> request) {
        Map<String, Object> payload = new java.util.HashMap<>(request);
        Long jobId = AiJobContext.getJobId();
        if (jobId == null) {
            return payload;
        }
        payload.put("jobId", jobId);
        payload.put("jobType", AiJobContext.getJobType());
        Object targetId = payload.get("targetId");
        if (targetId != null) {
            payload.put("targetReference", targetId);
        }
        return payload;
    }
}
