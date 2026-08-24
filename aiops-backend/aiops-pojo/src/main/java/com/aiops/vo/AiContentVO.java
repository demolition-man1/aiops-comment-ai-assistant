package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiContentVO {
    private Long contentId;
    private String generatedContent;
    private String modelName;
}

