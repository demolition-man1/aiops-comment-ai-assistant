package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_negative_reply")
public class BizNegativeReply {
    @TableId(type = IdType.AUTO)
    private Long id;
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
    private Integer ragUsed;
    private String ragReferences;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
