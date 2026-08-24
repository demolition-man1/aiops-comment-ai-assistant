package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentTranslationVO {
    private Long commentId;
    private String productId;
    private String originalContent;
    private String sourceLanguage;
    private String targetLanguage;
    private String translatedContent;
    private String modelName;
    private Boolean cached;
}
