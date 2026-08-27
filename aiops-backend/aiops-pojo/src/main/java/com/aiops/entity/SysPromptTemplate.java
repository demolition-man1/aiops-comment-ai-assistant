package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_prompt_template")
public class SysPromptTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateName;
    private String businessType;
    private String language;
    private String templateContent;
    private String variableSchema;
    private Integer defaultFlag;
    private Integer enabled;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
