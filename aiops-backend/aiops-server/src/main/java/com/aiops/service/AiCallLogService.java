package com.aiops.service;

import com.aiops.dto.AiCallLogQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.AiCallLogOverviewVO;
import com.aiops.vo.AiCallLogVO;

public interface AiCallLogService {
    PageResult<AiCallLogVO> pageLogs(AiCallLogQueryDTO queryDTO);

    AiCallLogOverviewVO overview(AiCallLogQueryDTO queryDTO);

    void record(Long userId, String businessType, String targetType, String targetId,
                Long promptTemplateId, String modelName, String callStatus,
                Integer tokenUsage, Long latencyMs, String errorMessage);
}
