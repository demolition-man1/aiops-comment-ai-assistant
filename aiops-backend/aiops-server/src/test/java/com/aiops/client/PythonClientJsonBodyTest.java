package com.aiops.client;

import com.aiops.properties.PythonServiceProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PythonClientJsonBodyTest {

    @Test
    void analysisClientSendsJsonBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PythonAnalysisClient client = new PythonAnalysisClient(builder.build(), properties(), new ObjectMapper());

        server.expect(requestTo("http://python-service/internal/analysis/comments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(content().json("{\"targetType\":\"product\",\"targetId\":\"demo-product\"}"))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        client.analyzeComments(Map.of("targetType", "product", "targetId", "demo-product"));

        server.verify();
    }

    @Test
    void shadowAnalysisClientSendsJsonBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PythonAnalysisClient client = new PythonAnalysisClient(builder.build(), properties(), new ObjectMapper());

        server.expect(requestTo("http://python-service/internal/analysis/comments/shadow"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(content().json("{\"taskId\":9,\"runId\":17}"))
                .andRespond(withSuccess("{\"success\":true,\"runId\":17}", MediaType.APPLICATION_JSON));

        client.analyzeCommentShadow(Map.of("taskId", 9, "runId", 17));

        server.verify();
    }

    @Test
    void aiClientSendsJsonBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PythonAiClient client = new PythonAiClient(builder.build(), properties(), new ObjectMapper());

        server.expect(requestTo("http://python-service/internal/ai/content"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(content().json("{\"targetType\":\"product\",\"targetId\":\"demo-product\"}"))
                .andRespond(withSuccess("{\"success\":true,\"generatedContent\":\"ok\",\"modelName\":\"test\"}", MediaType.APPLICATION_JSON));

        client.generateContent(Map.of("targetType", "product", "targetId", "demo-product"));

        server.verify();
    }

    @Test
    void aiClientSendsCommentTranslationJsonBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PythonAiClient client = new PythonAiClient(builder.build(), properties(), new ObjectMapper());

        server.expect(requestTo("http://python-service/internal/ai/comment-translate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(content().json("{\"commentId\":22,\"targetLanguage\":\"en-US\"}"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"translatedContent\":\"ok\",\"modelName\":\"test\"}}",
                        MediaType.APPLICATION_JSON
                ));

        client.translateComment(Map.of("commentId", 22, "targetLanguage", "en-US"));

        server.verify();
    }

    private PythonServiceProperties properties() {
        PythonServiceProperties properties = new PythonServiceProperties();
        properties.setBaseUrl("http://python-service");
        return properties;
    }
}
