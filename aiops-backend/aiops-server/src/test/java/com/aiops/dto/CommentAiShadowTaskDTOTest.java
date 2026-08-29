package com.aiops.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentAiShadowTaskDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsUnsupportedTargetAndOversizedSample() {
        CommentAiShadowTaskDTO request = new CommentAiShadowTaskDTO();
        request.setTargetType("category");
        request.setTargetId("office");
        request.setSampleSize(101);
        request.setMaxTotalTokens(1000);
        request.setLanguage("zh-CN");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void acceptsBoundedProductShadowRequest() {
        CommentAiShadowTaskDTO request = new CommentAiShadowTaskDTO();
        request.setTargetType("product");
        request.setTargetId("product-17");
        request.setSampleSize(60);
        request.setSampleSeed(20260829);
        request.setMaxTotalTokens(60000);
        request.setLanguage("en-US");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsExplicitlyMissingExecutionBounds() {
        CommentAiShadowTaskDTO request = new CommentAiShadowTaskDTO();
        request.setTargetType("seller");
        request.setTargetId("seller-17");
        request.setSampleSize(null);
        request.setMaxTotalTokens(null);
        request.setLanguage("pt-BR");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("sampleSize", "maxTotalTokens");
    }
}
