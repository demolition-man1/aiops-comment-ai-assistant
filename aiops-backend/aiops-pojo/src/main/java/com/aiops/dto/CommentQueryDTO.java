package com.aiops.dto;

import lombok.Data;

@Data
public class CommentQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String productId;
    private String sellerId;
    private String sentiment;
    private String problemType;
    private Integer minScore;
    private Integer maxScore;
    private Integer isNegative;
}
