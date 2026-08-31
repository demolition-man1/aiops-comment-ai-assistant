package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NegativeReplyVO {
    private Long replyId;
    private Long commentId;
    private String productId;
    private String sellerId;
    private String problemType;
    private String commentContent;
    private String toneType;
    private String replyContent;
    private String modelName;
    private String effectTag;
    private Integer useCount;
    private Integer favoriteFlag;
    private Boolean ragUsed;
    private List<RagReferenceVO> ragReferences;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
