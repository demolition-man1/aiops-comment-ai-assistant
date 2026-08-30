package com.aiops.service;

import com.aiops.dto.CommentAiAnnotationDTO;
import com.aiops.dto.CommentAiShadowTaskDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.CommentAiEvaluationVO;
import com.aiops.vo.CommentAiShadowResultVO;
import com.aiops.vo.CommentAiShadowRunVO;
import com.aiops.vo.CommentAiShadowTaskVO;

public interface CommentAiShadowService {
    CommentAiShadowTaskVO createTask(CommentAiShadowTaskDTO createDTO);

    CommentAiShadowTaskVO getTask(Long taskId);

    PageResult<CommentAiShadowRunVO> pageRuns(Integer pageNum, Integer pageSize, String targetType,
                                              String targetId, String runStatus);

    CommentAiShadowRunVO getRun(Long runId);

    PageResult<CommentAiShadowResultVO> pageResults(Long runId, Integer pageNum, Integer pageSize,
                                                    String annotationStatus);

    void upsertAnnotation(Long commentId, CommentAiAnnotationDTO annotationDTO);

    CommentAiEvaluationVO evaluateRun(Long runId);
}
