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
    public Map<String, Object> generateProductCompare(Map<String, Object> request) {
        return restClient.post()
                .uri(properties.getBaseUrl() + "/internal/ai/product-compare")
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
            throw new IllegalArgumentException("Failed to serialize Python AI request", exception);
        }
    }
}
