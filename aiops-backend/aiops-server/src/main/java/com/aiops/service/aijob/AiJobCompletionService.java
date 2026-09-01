package com.aiops.service.aijob;

import java.util.function.Supplier;

public interface AiJobCompletionService {

    AiJobExecutionResult complete(Long taskId, Supplier<AiJobExecutionResult> persistAction);
}
