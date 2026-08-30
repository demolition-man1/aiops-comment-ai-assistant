package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Data
@Schema(description = "评论 AI 人工标注参数")
public class CommentAiAnnotationDTO {
    @NotBlank(message = "人工情感不能为空")
    @Pattern(regexp = "positive|neutral|negative", message = "人工情感仅支持 positive、neutral 或 negative")
    private String manualSentiment;

    @NotNull(message = "人工问题标签不能为空")
    @Size(max = 5, message = "人工问题标签不能超过 5 个")
    private List<@NotBlank(message = "人工问题标签不能为空") @Size(max = 64, message = "人工问题标签长度不能超过 64") String> manualProblemTypes = new ArrayList<>();

    @Size(max = 500, message = "标注备注长度不能超过 500")
    private String annotationNote;

    @AssertTrue(message = "人工问题标签不能重复")
    public boolean isProblemTypesDistinct() {
        List<String> labels = normalizedProblemTypes();
        return labels.size() == (manualProblemTypes == null ? 0 : manualProblemTypes.size());
    }

    public List<String> normalizedProblemTypes() {
        if (manualProblemTypes == null) {
            return List.of();
        }
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (String label : manualProblemTypes) {
            if (label != null && !label.isBlank()) {
                labels.add(label.trim().toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(labels);
    }
}
