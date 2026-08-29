package com.aiops.service;

import com.aiops.dto.CommentAiShadowTaskDTO;
import com.aiops.vo.CommentAiShadowTaskVO;

public interface CommentAiShadowService {
    CommentAiShadowTaskVO createTask(CommentAiShadowTaskDTO createDTO);

    CommentAiShadowTaskVO getTask(Long taskId);
}
