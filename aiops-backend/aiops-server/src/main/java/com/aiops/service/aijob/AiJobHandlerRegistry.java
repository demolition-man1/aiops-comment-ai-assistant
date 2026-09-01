package com.aiops.service.aijob;

import com.aiops.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiJobHandlerRegistry {

    private final Map<String, AiJobHandler> handlers;

    public AiJobHandlerRegistry(List<AiJobHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                AiJobHandler::jobType,
                Function.identity()
        ));
    }

    public AiJobHandler require(String jobType) {
        AiJobHandler handler = handlers.get(jobType);
        if (handler == null) {
            throw new BusinessException(400, "不支持的 AI 任务类型");
        }
        return handler;
    }
}
