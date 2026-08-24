package com.aiops.dto;

import lombok.Data;

@Data
public class SellerQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String state;
    private Integer minScore;
}

