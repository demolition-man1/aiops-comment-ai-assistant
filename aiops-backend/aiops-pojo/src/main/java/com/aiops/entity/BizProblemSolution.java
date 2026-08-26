package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_problem_solution")
public class BizProblemSolution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String problemType;
    private String categoryNameEn;
    private String solutionTitle;
    private String solutionContent;
    private String keywords;
    private String sourceType;
    private Integer priority;
    private Integer useCount;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
