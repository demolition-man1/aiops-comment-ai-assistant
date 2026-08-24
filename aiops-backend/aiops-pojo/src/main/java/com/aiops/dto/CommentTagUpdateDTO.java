package com.aiops.dto;

import lombok.Data;

import java.util.List;

@Data
public class CommentTagUpdateDTO {
    private String manualProblemType;
    private List<String> customTags;
}
