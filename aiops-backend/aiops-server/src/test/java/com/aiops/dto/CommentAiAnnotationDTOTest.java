package com.aiops.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommentAiAnnotationDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidAnnotationAndNormalizesDistinctProblemTypes() {
        CommentAiAnnotationDTO dto = new CommentAiAnnotationDTO();
        dto.setManualSentiment("negative");
        dto.setManualProblemTypes(List.of(" Delivery ", "quality"));

        assertThat(validator.validate(dto)).isEmpty();
        assertThat(dto.normalizedProblemTypes()).containsExactly("delivery", "quality");
    }

    @Test
    void rejectsUnsupportedSentimentDuplicateLabelsAndMoreThanFiveLabels() {
        CommentAiAnnotationDTO invalidSentiment = new CommentAiAnnotationDTO();
        invalidSentiment.setManualSentiment("mixed");
        invalidSentiment.setManualProblemTypes(List.of());

        CommentAiAnnotationDTO duplicateLabels = new CommentAiAnnotationDTO();
        duplicateLabels.setManualSentiment("negative");
        duplicateLabels.setManualProblemTypes(List.of("delivery", "Delivery"));

        CommentAiAnnotationDTO tooManyLabels = new CommentAiAnnotationDTO();
        tooManyLabels.setManualSentiment("negative");
        tooManyLabels.setManualProblemTypes(List.of("a", "b", "c", "d", "e", "f"));

        assertThat(validator.validate(invalidSentiment)).isNotEmpty();
        assertThat(validator.validate(duplicateLabels)).isNotEmpty();
        assertThat(validator.validate(tooManyLabels)).isNotEmpty();
    }
}
