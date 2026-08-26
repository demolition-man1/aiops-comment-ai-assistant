package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_custom_tag")
public class BizCustomTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagName;
    private String tagGroup;
    private String color;
    private String description;
    private Integer sortOrder;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
