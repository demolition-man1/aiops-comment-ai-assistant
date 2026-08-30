package com.aiops.client;

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
public class PythonAnalysisClient {

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
    public Map<String, Object> importCsv(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/csv/import")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> importByCrawler(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/crawler/import")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeComments(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/analysis/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeCommentShadow(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/analysis/comments/shadow")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> evaluateCommentShadow(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/analysis/comments/evaluation")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonBody(request))
                .retrieve()
                .body(Map.class);
    }

    private String jsonBody(Map<String, Object> request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize Python analysis request", exception);
        }
    }
}
