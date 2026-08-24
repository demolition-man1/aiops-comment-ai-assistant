package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private Long id;
    private String reviewId;
    private String productId;
    private String sellerId;
    private Integer reviewScore;
    private String reviewTitle;
    private String reviewContent;
    private String cleanContent;
    private String sentiment;
    private String systemProblemType;
    private String manualProblemType;
    private String effectiveProblemType;
    private List<String> customTags;
    private Integer isNegative;
    private LocalDateTime reviewTime;
}
