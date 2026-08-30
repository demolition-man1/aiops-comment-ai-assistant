package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentAiHybridReadinessVO {
    private Boolean ready;
    private List<String> failures;
    private Integer eligibleDecisionCount;
    private Integer activeDecisionCount;
    private String mode;
}
