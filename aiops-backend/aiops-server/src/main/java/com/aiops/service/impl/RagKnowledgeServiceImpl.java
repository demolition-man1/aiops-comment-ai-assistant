package com.aiops.service.impl;

import com.aiops.client.PythonAiClient;
import com.aiops.exception.BusinessException;
import com.aiops.service.RagKnowledgeService;
import com.aiops.vo.RagIndexStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RagKnowledgeServiceImpl implements RagKnowledgeService {

    private final PythonAiClient pythonAiClient;

    @Override
    public RagIndexStatusVO getStatus() {
        return callPython(pythonAiClient::getRagStatus);
    }

    @Override
    public RagIndexStatusVO reindex() {
        return callPython(pythonAiClient::reindexRag);
    }

    private RagIndexStatusVO callPython(Supplier<Map<String, Object>> call) {
        try {
            Map<String, Object> response = call.get();
            if (response == null || Boolean.FALSE.equals(response.get("success"))) {
                throw new BusinessException(503, "知识索引服务暂时不可用");
            }
            return toStatus(data(response));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new BusinessException(409, "知识索引正在重建或当前不可用，请稍后重试");
            }
            throw new BusinessException(503, "知识索引服务暂时不可用");
        } catch (Exception exception) {
            throw new BusinessException(503, "知识索引服务暂时不可用");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> response) {
        Object value = response.get("data");
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    private RagIndexStatusVO toStatus(Map<String, Object> data) {
        return new RagIndexStatusVO(
                booleanValue(data.get("enabled")),
                booleanValue(data.get("ready")),
                stringValue(data.get("state")),
                stringValue(data.get("collection")),
                intValue(data.get("documentCount")),
                intValue(data.get("problemSolutionCount")),
                intValue(data.get("historicalReplyCount")),
                stringValue(data.get("embeddingModel")),
                stringValue(data.get("lastReindexAt")),
                stringValue(data.get("lastError"))
        );
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
