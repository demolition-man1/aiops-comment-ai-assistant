package com.aiops.service;

import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.dto.ProductCompareDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.AiJobCreatedVO;
import com.aiops.vo.AiJobVO;

public interface AiJobService {

    AiJobCreatedVO createReportJob(AiReportGenerateDTO dto, String idempotencyKey);

    AiJobCreatedVO createProductCompareJob(ProductCompareDTO dto, String idempotencyKey);

    AiJobVO getOwnedJob(Long jobId);

    PageResult<AiJobVO> pageOwnedJobs(Integer pageNum, Integer pageSize, String jobType, String taskStatus);
}
