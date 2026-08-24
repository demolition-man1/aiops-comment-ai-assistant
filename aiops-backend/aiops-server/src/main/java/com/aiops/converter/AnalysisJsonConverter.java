package com.aiops.converter;

import com.aiops.vo.DistributionItemVO;
import com.aiops.vo.KeywordItemVO;
import com.aiops.vo.TrendItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AnalysisJsonConverter {

    private static final TypeReference<List<KeywordItemVO>> KEYWORD_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<DistributionItemVO>> DISTRIBUTION_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<TrendItemVO>> TREND_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public List<KeywordItemVO> parseKeywords(String json) {
        return readList(json, KEYWORD_LIST_TYPE);
    }

    public List<DistributionItemVO> parseDistributions(String json) {
        return readList(json, DISTRIBUTION_LIST_TYPE);
    }

    public List<TrendItemVO> parseTrends(String json) {
        return readList(json, TREND_LIST_TYPE);
    }

    public List<String> parseStringList(String json) {
        return readList(json, STRING_LIST_TYPE);
    }

    public String toJsonArray(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<T> list = objectMapper.readValue(json, typeReference);
            return list == null ? List.of() : list;
        } catch (Exception exception) {
            return List.of();
        }
    }
}
