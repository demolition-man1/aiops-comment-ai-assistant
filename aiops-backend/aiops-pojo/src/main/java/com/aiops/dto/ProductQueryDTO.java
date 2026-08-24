package com.aiops.dto;

import lombok.Data;

@Data
public class ProductQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String categoryNameEn;
    private Integer minScore;
    private Integer maxScore;
    private String keyword;
}

