package com.aiops.service.impl;

import com.aiops.client.PythonAnalysisClient;
import com.aiops.context.BaseContext;
import com.aiops.dto.CommentAiShadowTaskDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.entity.BizCommentAiShadowRun;
import com.aiops.entity.SysPromptTemplate;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCommentAiShadowRunMapper;
import com.aiops.service.AiCallLogService;
import com.aiops.service.CommentAiShadowService;
import com.aiops.service.PromptTemplateService;
import com.aiops.vo.CommentAiShadowTaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentAiShadowServiceImpl implements CommentAiShadowService {

    private static final String TASK_TYPE = "comment_ai_shadow";
    private static final String PROMPT_BUSINESS_TYPE = "comment_analysis_shadow";

    private final BizAnalysisTaskMapper taskMapper;
    private final BizCommentAiShadowRunMapper runMapper;
    private final PythonAnalysisClient pythonAnalysisClient;
    private final PromptTemplateService promptTemplateService;
    private final AiCallLogService aiCallLogService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;

    @Override
    public CommentAiShadowTaskVO createTask(CommentAiShadowTaskDTO createDTO) {
        validateCreateDTO(createDTO);
        SysPromptTemplate template = promptTemplateService
                .findDefaultTemplate(PROMPT_BUSINESS_TYPE, createDTO.getLanguage())
                .orElseThrow(() -> new BusinessException(422, "未找到评论 Shadow 分析 Prompt 模板"));
        Long userId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();

        BizAnalysisTask task = new BizAnalysisTask();
        task.setUserId(userId);
        task.setTargetType(createDTO.getTargetType());
        task.setTargetId(createDTO.getTargetId());
        task.setTaskType(TASK_TYPE);
        task.setTaskStatus("processing");
        task.setProgress(0);
        task.setRequestParam(toJson(createDTO));
        task.setStartTime(now);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        taskMapper.insert(task);

        BizCommentAiShadowRun run = new BizCommentAiShadowRun();
        run.setTaskId(task.getId());
        run.setUserId(userId);
        run.setTargetType(createDTO.getTargetType());
        run.setTargetId(createDTO.getTargetId());
        run.setSampleSeed(createDTO.getSampleSeed());
        run.setRequestedSampleSize(createDTO.getSampleSize());
        run.setActualSampleSize(0);
        run.setMaxTotalTokens(createDTO.getMaxTotalTokens());
        run.setTotalCalls(0);
        run.setSuccessCount(0);
        run.setFailureCount(0);
        run.setTotalTokens(0);
        run.setLatencyMs(0L);
        run.setRunStatus("processing");
        run.setCreateTime(now);
        run.setUpdateTime(now);
        runMapper.insert(run);

        Map<String, Object> request = buildPythonRequest(task, run, createDTO, template);
        try {
            taskExecutor.execute(() -> callPythonShadow(task.getId(), run.getId(), userId,
                    createDTO.getTargetType(), createDTO.getTargetId(), template.getId(), request));
        } catch (RuntimeException exception) {
            markFailed(task.getId(), run.getId(), exception.getMessage());
            return new CommentAiShadowTaskVO(task.getId(), run.getId(), "failed", 100, 0, exception.getMessage());
        }
        return new CommentAiShadowTaskVO(task.getId(), run.getId(), "processing", 0, 0, null);
    }

    @Override
    public CommentAiShadowTaskVO getTask(Long taskId) {
        BizAnalysisTask task = taskMapper.selectById(taskId);
        if (task == null || !TASK_TYPE.equals(task.getTaskType())) {
            throw new BusinessException(404, "评论 Shadow 任务不存在");
        }
        BizCommentAiShadowRun run = runMapper.selectOne(new LambdaQueryWrapper<BizCommentAiShadowRun>()
                .eq(BizCommentAiShadowRun::getTaskId, taskId)
                .last("limit 1"));
        return new CommentAiShadowTaskVO(task.getId(), run == null ? null : run.getId(), task.getTaskStatus(),
                task.getProgress(), run == null ? 0 : run.getActualSampleSize(), task.getErrorMessage());
    }

    private void callPythonShadow(Long taskId, Long runId, Long userId, String targetType, String targetId,
                                  Long promptTemplateId, Map<String, Object> request) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> response = pythonAnalysisClient.analyzeCommentShadow(request);
            String status = stringValue(response, "status", "failed");
            boolean success = Boolean.TRUE.equals(response == null ? null : response.get("success"));
            if (!success || !isTerminalStatus(status)) {
                throw new BusinessException(503, stringValue(response, "message", "Python Shadow 分析服务返回失败"));
            }
            updateTask(taskId, status, 100, stringValue(response, "message", null));
            aiCallLogService.record(userId, TASK_TYPE, targetType, targetId, promptTemplateId,
                    stringValue(response, "modelName", null), "failed".equals(status) ? "failed" : "success",
                    intValue(response, "totalTokens", 0), elapsedMs(startedAt), stringValue(response, "message", null));
        } catch (Exception exception) {
            String errorMessage = exception instanceof BusinessException
                    ? exception.getMessage()
                    : "Python Shadow 分析服务不可用：" + exception.getMessage();
            markFailed(taskId, runId, errorMessage);
            aiCallLogService.record(userId, TASK_TYPE, targetType, targetId, promptTemplateId,
                    null, "failed", 0, elapsedMs(startedAt), errorMessage);
        }
    }

    private void markFailed(Long taskId, Long runId, String errorMessage) {
        updateTask(taskId, "failed", 100, errorMessage);
        BizCommentAiShadowRun run = new BizCommentAiShadowRun();
        run.setId(runId);
        run.setRunStatus("failed");
        run.setErrorMessage(errorMessage);
        run.setEndTime(LocalDateTime.now());
        run.setUpdateTime(LocalDateTime.now());
        runMapper.updateById(run);
    }

    private void updateTask(Long taskId, String status, int progress, String errorMessage) {
        BizAnalysisTask task = new BizAnalysisTask();
        task.setId(taskId);
        task.setTaskStatus(status);
        task.setProgress(progress);
        task.setErrorMessage(errorMessage);
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private Map<String, Object> buildPythonRequest(BizAnalysisTask task, BizCommentAiShadowRun run,
                                                   CommentAiShadowTaskDTO dto, SysPromptTemplate template) {
        Map<String, Object> request = new HashMap<>();
        request.put("taskId", task.getId());
        request.put("runId", run.getId());
        request.put("targetType", dto.getTargetType());
        request.put("targetId", dto.getTargetId());
        request.put("sampleSize", dto.getSampleSize());
        request.put("sampleSeed", dto.getSampleSeed());
        request.put("maxTotalTokens", dto.getMaxTotalTokens());
        request.put("promptTemplateId", template.getId());
        request.put("promptTemplate", template.getTemplateContent());
        request.put("promptVariables", Map.of(
                "targetType", dto.getTargetType(),
                "targetId", dto.getTargetId(),
                "language", dto.getLanguage()
        ));
        return request;
    }

    private void validateCreateDTO(CommentAiShadowTaskDTO dto) {
        if (dto == null) {
            throw new BusinessException(400, "评论 Shadow 任务参数不能为空");
        }
        if (!"product".equals(dto.getTargetType()) && !"seller".equals(dto.getTargetType())) {
            throw new BusinessException(400, "目标类型仅支持 product 或 seller");
        }
        if (dto.getTargetId() == null || dto.getTargetId().isBlank()) {
            throw new BusinessException(400, "目标 ID 不能为空");
        }
        if (dto.getSampleSize() == null || dto.getSampleSize() < 1 || dto.getSampleSize() > 100) {
            throw new BusinessException(400, "样本量必须在 1 到 100 之间");
        }
        if (dto.getSampleSeed() == null) {
            throw new BusinessException(400, "抽样种子不能为空");
        }
        if (dto.getMaxTotalTokens() == null || dto.getMaxTotalTokens() < 1000 || dto.getMaxTotalTokens() > 100000) {
            throw new BusinessException(400, "Token 预算必须在 1000 到 100000 之间");
        }
        if (!"zh-CN".equals(dto.getLanguage()) && !"en-US".equals(dto.getLanguage()) && !"pt-BR".equals(dto.getLanguage())) {
            throw new BusinessException(400, "语言仅支持 zh-CN、en-US 或 pt-BR");
        }
        dto.setTargetId(dto.getTargetId().trim());
    }

    private boolean isTerminalStatus(String status) {
        return "success".equals(status) || "partial".equals(status) || "budget_stopped".equals(status);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "评论 Shadow 任务参数序列化失败");
        }
    }

    private static Integer intValue(Map<String, Object> response, String key, Integer defaultValue) {
        if (response == null || response.get(key) == null) {
            return defaultValue;
        }
        Object value = response.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String stringValue(Map<String, Object> response, String key, String defaultValue) {
        if (response == null || response.get(key) == null) {
            return defaultValue;
        }
        String value = String.valueOf(response.get(key)).trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private static long elapsedMs(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
