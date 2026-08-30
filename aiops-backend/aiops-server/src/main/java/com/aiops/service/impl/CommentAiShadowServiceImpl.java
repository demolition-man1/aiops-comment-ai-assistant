package com.aiops.service.impl;

import com.aiops.client.PythonAnalysisClient;
import com.aiops.context.BaseContext;
import com.aiops.dto.CommentAiAnnotationDTO;
import com.aiops.dto.CommentAiHybridActivationDTO;
import com.aiops.dto.CommentAiShadowTaskDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.entity.BizCommentAiAnnotation;
import com.aiops.entity.BizCommentAiDecision;
import com.aiops.entity.BizCommentAiShadowRun;
import com.aiops.entity.SysPromptTemplate;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCommentAiAnnotationMapper;
import com.aiops.mapper.BizCommentAiDecisionMapper;
import com.aiops.mapper.BizCommentAiShadowResultMapper;
import com.aiops.mapper.BizCommentAiShadowRunMapper;
import com.aiops.result.PageResult;
import com.aiops.service.AiCallLogService;
import com.aiops.service.CommentAiShadowService;
import com.aiops.service.PromptTemplateService;
import com.aiops.properties.CommentAiHybridProperties;
import com.aiops.vo.CommentAiEvaluationVO;
import com.aiops.vo.CommentAiHybridReadinessVO;
import com.aiops.vo.CommentAiShadowResultVO;
import com.aiops.vo.CommentAiShadowRunVO;
import com.aiops.vo.CommentAiShadowTaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentAiShadowServiceImpl implements CommentAiShadowService {

    private static final String TASK_TYPE = "comment_ai_shadow";
    private static final String PROMPT_BUSINESS_TYPE = "comment_analysis_shadow";

    private final BizAnalysisTaskMapper taskMapper;
    private final BizCommentAiShadowRunMapper runMapper;
    private final BizCommentAiShadowResultMapper resultMapper;
    private final BizCommentAiAnnotationMapper annotationMapper;
    private final BizCommentAiDecisionMapper decisionMapper;
    private final PythonAnalysisClient pythonAnalysisClient;
    private final PromptTemplateService promptTemplateService;
    private final AiCallLogService aiCallLogService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;
    private final CommentAiHybridProperties hybridProperties;

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

    @Override
    public PageResult<CommentAiShadowRunVO> pageRuns(Integer pageNum, Integer pageSize, String targetType,
                                                     String targetId, String runStatus) {
        int current = normalizePageNum(pageNum);
        int size = normalizePageSize(pageSize);
        Page<BizCommentAiShadowRun> page = runMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BizCommentAiShadowRun>()
                        .eq(nonBlank(targetType), BizCommentAiShadowRun::getTargetType, trimToNull(targetType))
                        .eq(nonBlank(targetId), BizCommentAiShadowRun::getTargetId, trimToNull(targetId))
                        .eq(nonBlank(runStatus), BizCommentAiShadowRun::getRunStatus, trimToNull(runStatus))
                        .orderByDesc(BizCommentAiShadowRun::getCreateTime));
        return PageResult.of(page.getRecords().stream().map(this::toRunVO).toList(),
                page.getTotal(), current, size);
    }

    @Override
    public CommentAiShadowRunVO getRun(Long runId) {
        return toRunVO(requireRun(runId));
    }

    @Override
    public PageResult<CommentAiShadowResultVO> pageResults(Long runId, Integer pageNum, Integer pageSize,
                                                           String annotationStatus) {
        requireRun(runId);
        String filter = annotationStatus == null ? "all" : annotationStatus.trim().toLowerCase();
        if (!"all".equals(filter) && !"annotated".equals(filter) && !"unannotated".equals(filter)) {
            throw new BusinessException(400, "标注状态仅支持 all、annotated 或 unannotated");
        }
        List<Map<String, Object>> matched = resultMapper.selectEvaluationRows(runId).stream()
                .filter(row -> !"annotated".equals(filter) || nonBlank(stringValue(row, "manualSentiment", null)))
                .filter(row -> !"unannotated".equals(filter) || !nonBlank(stringValue(row, "manualSentiment", null)))
                .toList();
        int current = normalizePageNum(pageNum);
        int size = normalizePageSize(pageSize);
        int from = Math.min((current - 1) * size, matched.size());
        int to = Math.min(from + size, matched.size());
        return PageResult.of(matched.subList(from, to).stream().map(this::toResultVO).toList(),
                matched.size(), current, size);
    }

    @Override
    public void upsertAnnotation(Long commentId, CommentAiAnnotationDTO annotationDTO) {
        if (commentId == null || commentId < 1 || annotationDTO == null) {
            throw new BusinessException(400, "评论标注参数不完整");
        }
        BizCommentAiAnnotation annotation = annotationMapper.selectOne(new LambdaQueryWrapper<BizCommentAiAnnotation>()
                .eq(BizCommentAiAnnotation::getCommentId, commentId).last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (annotation == null) {
            annotation = new BizCommentAiAnnotation();
            annotation.setCommentId(commentId);
            annotation.setAnnotatedBy(BaseContext.getCurrentId());
            annotation.setAnnotationTime(now);
            setAnnotationFields(annotation, annotationDTO, now);
            annotationMapper.insert(annotation);
            return;
        }
        setAnnotationFields(annotation, annotationDTO, now);
        annotationMapper.updateById(annotation);
    }

    @Override
    public CommentAiEvaluationVO evaluateRun(Long runId) {
        BizCommentAiShadowRun run = requireRun(runId);
        List<Map<String, Object>> rows = resultMapper.selectEvaluationRows(runId);
        long annotatedCount = rows.stream()
                .filter(row -> nonBlank(stringValue(row, "manualSentiment", null))).count();
        if (annotatedCount == 0) {
            return localEvaluation(rows, run.getRunStatus());
        }
        Map<String, Object> response = pythonAnalysisClient.evaluateCommentShadow(Map.of(
                "rows", rows.stream().map(this::toEvaluationRow).toList()));
        if (!Boolean.TRUE.equals(response == null ? null : response.get("success"))
                || !(response.get("evaluation") instanceof Map<?, ?> evaluation)) {
            throw new BusinessException(503, "评论 Shadow 评估服务返回失败");
        }
        return toEvaluationVO(evaluation, run.getRunStatus());
    }

    @Override
    public CommentAiHybridReadinessVO hybridReadiness(Long runId) {
        requireRun(runId);
        CommentAiHybridReadinessVO base = new CommentAiHybridGate(hybridProperties).evaluate(evaluateRun(runId));
        int eligibleCount = decisionMapper.selectEligibleCandidates(runId,
                BigDecimal.valueOf(hybridProperties.getMinConfidence())).size();
        int activeCount = valueOrZero(decisionMapper.selectActiveCountByRunId(runId));
        List<String> failures = new ArrayList<>(base.getFailures());
        if (base.getReady() && eligibleCount == 0) {
            failures.add("no_eligible_decisions");
        }
        return new CommentAiHybridReadinessVO(failures.isEmpty(), List.copyOf(failures), eligibleCount, activeCount,
                hybridProperties.getMode());
    }

    @Override
    @Transactional
    public CommentAiHybridReadinessVO activateHybrid(Long runId, CommentAiHybridActivationDTO activationDTO) {
        if (activationDTO == null || !Boolean.TRUE.equals(activationDTO.getConfirmed())) {
            throw new BusinessException(400, "必须显式确认启用 Hybrid 问题分类");
        }
        CommentAiHybridReadinessVO readiness = hybridReadiness(runId);
        if (!Boolean.TRUE.equals(readiness.getReady())) {
            throw new BusinessException(422, "当前运行未通过 Hybrid 准入：" + String.join(", ", readiness.getFailures()));
        }
        Long userId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> candidate : decisionMapper.selectEligibleCandidates(runId,
                BigDecimal.valueOf(hybridProperties.getMinConfidence()))) {
            Long commentId = longValue(candidate, "commentId", null);
            if (commentId == null) {
                continue;
            }
            BizCommentAiDecision decision = decisionMapper.selectOne(new LambdaQueryWrapper<BizCommentAiDecision>()
                    .eq(BizCommentAiDecision::getCommentId, commentId).last("limit 1"));
            boolean existing = decision != null;
            if (!existing) {
                decision = new BizCommentAiDecision();
                decision.setCommentId(commentId);
            }
            decision.setShadowResultId(longValue(candidate, "shadowResultId", null));
            decision.setAcceptedProblemType(trimToNull(stringValue(candidate, "acceptedProblemType", null)));
            decision.setConfidence(decimalValue(candidate.get("confidence")));
            decision.setGateVersion("v1");
            decision.setActive(1);
            decision.setActivatedBy(userId);
            decision.setActivatedAt(now);
            if (existing) {
                decisionMapper.updateById(decision);
            } else {
                decisionMapper.insert(decision);
            }
        }
        return hybridReadiness(runId);
    }

    private void setAnnotationFields(BizCommentAiAnnotation annotation, CommentAiAnnotationDTO dto, LocalDateTime now) {
        annotation.setManualSentiment(dto.getManualSentiment().trim().toLowerCase());
        annotation.setManualProblemTypes(toJson(dto.normalizedProblemTypes()));
        annotation.setAnnotationNote(trimToNull(dto.getAnnotationNote()));
        annotation.setUpdateTime(now);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private BizCommentAiShadowRun requireRun(Long runId) {
        if (runId == null || runId < 1) {
            throw new BusinessException(400, "运行记录 ID 不合法");
        }
        BizCommentAiShadowRun run = runMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException(404, "评论 Shadow 运行记录不存在");
        }
        return run;
    }

    private CommentAiShadowRunVO toRunVO(BizCommentAiShadowRun run) {
        return new CommentAiShadowRunVO(run.getId(), run.getTaskId(), run.getTargetType(), run.getTargetId(),
                run.getSampleSeed(), run.getRequestedSampleSize(), run.getActualSampleSize(), run.getMaxTotalTokens(),
                run.getTotalCalls(), run.getSuccessCount(), run.getFailureCount(), run.getTotalTokens(), run.getLatencyMs(),
                run.getRunStatus(), run.getErrorMessage(), run.getStartTime(), run.getEndTime(), run.getCreateTime());
    }

    private CommentAiShadowResultVO toResultVO(Map<String, Object> row) {
        CommentAiShadowResultVO result = new CommentAiShadowResultVO();
        result.setResultId(longValue(row, "resultId", null));
        result.setRunId(longValue(row, "runId", null));
        result.setCommentId(longValue(row, "commentId", null));
        result.setSampleOrder(intValue(row, "sampleOrder", null));
        result.setReviewScore(intValue(row, "reviewScore", null));
        result.setReviewContent(stringValue(row, "reviewContent", null));
        result.setRuleSentiment(stringValue(row, "ruleSentiment", null));
        result.setRuleProblemType(stringValue(row, "ruleProblemType", null));
        result.setAiSentiment(stringValue(row, "aiSentiment", null));
        result.setAiSentimentConfidence(decimalValue(row.get("aiSentimentConfidence")));
        result.setAiPrimaryProblem(stringValue(row, "aiPrimaryProblem", null));
        result.setAiProblems(labels(row.get("aiProblems")));
        result.setAiEvidence(stringValue(row, "aiEvidence", null));
        result.setJsonValid(intValue(row, "jsonValid", 0));
        result.setEvidenceValid(intValue(row, "evidenceValid", 0));
        result.setCallStatus(stringValue(row, "callStatus", "pending"));
        result.setModelName(stringValue(row, "modelName", null));
        result.setTokenUsage(intValue(row, "tokenUsage", 0));
        result.setTokenUsageEstimated(intValue(row, "tokenUsageEstimated", 0));
        result.setLatencyMs(longValue(row, "latencyMs", 0L));
        result.setErrorMessage(stringValue(row, "errorMessage", null));
        result.setManualSentiment(stringValue(row, "manualSentiment", null));
        result.setManualProblemTypes(labels(row.get("manualProblemTypes")));
        result.setAnnotationNote(stringValue(row, "annotationNote", null));
        if (row.get("annotationTime") instanceof LocalDateTime time) {
            result.setAnnotationTime(time);
        }
        return result;
    }

    private Map<String, Object> toEvaluationRow(Map<String, Object> row) {
        Map<String, Object> requestRow = new HashMap<>();
        requestRow.put("manualSentiment", trimToNull(stringValue(row, "manualSentiment", null)));
        requestRow.put("manualProblemTypes", labels(row.get("manualProblemTypes")));
        requestRow.put("ruleSentiment", trimToNull(stringValue(row, "ruleSentiment", null)));
        requestRow.put("ruleProblemType", trimToNull(stringValue(row, "ruleProblemType", null)));
        requestRow.put("aiSentiment", trimToNull(stringValue(row, "aiSentiment", null)));
        requestRow.put("aiProblems", labels(row.get("aiProblems")));
        requestRow.put("callStatus", stringValue(row, "callStatus", "pending"));
        requestRow.put("jsonValid", intValue(row, "jsonValid", 0) == 1);
        requestRow.put("evidenceValid", intValue(row, "evidenceValid", 0) == 1);
        requestRow.put("tokenUsage", intValue(row, "tokenUsage", 0));
        requestRow.put("tokenUsageEstimated", intValue(row, "tokenUsageEstimated", 0) == 1);
        requestRow.put("latencyMs", longValue(row, "latencyMs", 0L));
        return requestRow;
    }

    private CommentAiEvaluationVO localEvaluation(List<Map<String, Object>> rows, String runStatus) {
        int sampleCount = rows.size();
        int attempted = (int) rows.stream().filter(row -> !"pending".equals(stringValue(row, "callStatus", "pending"))).count();
        int successful = (int) rows.stream().filter(row -> "success".equals(stringValue(row, "callStatus", "pending"))).count();
        int jsonValid = (int) rows.stream().filter(row -> intValue(row, "jsonValid", 0) == 1).count();
        int evidenceValid = (int) rows.stream().filter(row -> "success".equals(stringValue(row, "callStatus", "pending"))
                && intValue(row, "evidenceValid", 0) == 1).count();
        int totalTokens = rows.stream().mapToInt(row -> intValue(row, "tokenUsage", 0)).sum();
        int estimated = (int) rows.stream().filter(row -> intValue(row, "tokenUsageEstimated", 0) == 1).count();
        long totalLatency = rows.stream().filter(row -> !"pending".equals(stringValue(row, "callStatus", "pending")))
                .mapToLong(row -> longValue(row, "latencyMs", 0L)).sum();

        CommentAiEvaluationVO evaluation = new CommentAiEvaluationVO();
        evaluation.setQualityReady(false);
        evaluation.setSampleCount(sampleCount);
        evaluation.setAnnotatedCount(0);
        evaluation.setAttemptedCallCount(attempted);
        evaluation.setSuccessfulCallCount(successful);
        evaluation.setFailedCallCount(Math.max(0, attempted - successful));
        evaluation.setAnnotationCoverage(rate(0, sampleCount));
        evaluation.setJsonValidRate(rate(jsonValid, attempted));
        evaluation.setEvidenceValidRate(rate(evidenceValid, successful));
        evaluation.setCallSuccessRate(rate(successful, attempted));
        evaluation.setTotalTokens(totalTokens);
        evaluation.setEstimatedTokenRowCount(estimated);
        evaluation.setAverageLatencyMs(attempted == 0 ? 0D : (double) totalLatency / attempted);
        evaluation.setBudgetStopped("budget_stopped".equals(runStatus));
        evaluation.setRule(emptyMetricBlock());
        evaluation.setAi(emptyMetricBlock());
        evaluation.setDelta(emptyMetricBlock());
        return evaluation;
    }

    private CommentAiEvaluationVO toEvaluationVO(Map<?, ?> evaluation, String runStatus) {
        Map<?, ?> counts = mapValue(evaluation.get("counts"));
        Map<?, ?> validity = mapValue(evaluation.get("validity"));
        Map<?, ?> usage = mapValue(evaluation.get("usage"));
        CommentAiEvaluationVO result = new CommentAiEvaluationVO();
        result.setQualityReady(boolValue(evaluation.get("qualityReady")));
        result.setSampleCount(intValue(counts, "sampleCount", 0));
        result.setAnnotatedCount(intValue(counts, "annotatedCount", 0));
        result.setAttemptedCallCount(intValue(counts, "attemptedCallCount", 0));
        result.setSuccessfulCallCount(intValue(counts, "successfulCallCount", 0));
        result.setFailedCallCount(intValue(counts, "failedCallCount", 0));
        result.setAnnotationCoverage(doubleValue(validity.get("annotationCoverage")));
        result.setJsonValidRate(doubleValue(validity.get("jsonValidRate")));
        result.setEvidenceValidRate(doubleValue(validity.get("evidenceValidRate")));
        result.setCallSuccessRate(doubleValue(validity.get("callSuccessRate")));
        result.setTotalTokens(intValue(usage, "totalTokens", 0));
        result.setEstimatedTokenRowCount(intValue(usage, "estimatedTokenRowCount", 0));
        result.setAverageLatencyMs(doubleValue(usage.get("averageLatencyMs")));
        result.setBudgetStopped("budget_stopped".equals(runStatus));
        result.setRule(metricBlock(mapValue(evaluation.get("rule"))));
        result.setAi(metricBlock(mapValue(evaluation.get("ai"))));
        result.setDelta(metricBlock(mapValue(evaluation.get("delta"))));
        return result;
    }

    private CommentAiEvaluationVO.MetricBlock metricBlock(Map<?, ?> source) {
        CommentAiEvaluationVO.MetricBlock result = new CommentAiEvaluationVO.MetricBlock();
        result.setSentimentAccuracy(doubleValue(source.get("sentimentAccuracy")));
        result.setProblemMicroF1(doubleValue(source.get("problemMicroF1")));
        result.setProblemMacroF1(doubleValue(source.get("problemMacroF1")));
        return result;
    }

    private CommentAiEvaluationVO.MetricBlock emptyMetricBlock() {
        return new CommentAiEvaluationVO.MetricBlock();
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

    private List<String> labels(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }
        if (rawValue instanceof List<?> values) {
            return values.stream().map(this::labelValue).filter(CommentAiShadowServiceImpl::nonBlank).distinct().toList();
        }
        String json = String.valueOf(rawValue).trim();
        if (json.isEmpty() || "null".equalsIgnoreCase(json)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.isArray()) {
                List<String> labels = new ArrayList<>();
                for (JsonNode item : root) {
                    String label = item.isObject() ? item.path("type").asText() : item.asText();
                    if (nonBlank(label) && !labels.contains(label.trim())) {
                        labels.add(label.trim());
                    }
                }
                return List.copyOf(labels);
            }
            return nonBlank(root.asText()) ? List.of(root.asText().trim()) : List.of();
        } catch (JsonProcessingException ignored) {
            return List.of(json);
        }
    }

    private String labelValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return stringValue(map, "type", null);
        }
        return value == null ? null : String.valueOf(value).trim();
    }

    private static Map<?, ?> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static boolean boolValue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static Double doubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long longValue(Map<?, ?> response, String key, Long defaultValue) {
        if (response == null || response.get(key) == null) {
            return defaultValue;
        }
        Object value = response.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static Double rate(long numerator, long denominator) {
        return denominator == 0 ? 0D : (double) numerator / denominator;
    }

    private static int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private static int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        return nonBlank(value) ? value.trim() : null;
    }

    private static Integer intValue(Map<?, ?> response, String key, Integer defaultValue) {
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

    private static String stringValue(Map<?, ?> response, String key, String defaultValue) {
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
