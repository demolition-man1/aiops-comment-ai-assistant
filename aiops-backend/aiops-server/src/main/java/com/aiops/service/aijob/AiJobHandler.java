package com.aiops.service.aijob;

import com.aiops.entity.BizAnalysisTask;

public interface AiJobHandler {

    String jobType();

    AiJobExecutionResult execute(BizAnalysisTask task);
}
