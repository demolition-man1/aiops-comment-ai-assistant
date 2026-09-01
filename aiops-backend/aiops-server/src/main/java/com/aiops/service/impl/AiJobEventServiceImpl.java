package com.aiops.service.impl;

import com.aiops.context.BaseContext;
import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.enumeration.AiJobStageEnum;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.service.AiJobEventService;
import com.aiops.vo.AiJobEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@RequiredArgsConstructor
public class AiJobEventServiceImpl implements AiJobEventService {

    private static final long EMITTER_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final BizAnalysisTaskMapper taskMapper;
    private final BizAiExecutionDetailMapper executionDetailMapper;
    private final Map<Long, Set<SseEmitter>> emittersByJob = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Long jobId, Long lastEventId) {
        BizAnalysisTask task = requireOwnedTask(jobId);
        BizAiExecutionDetail detail = executionDetailMapper.selectById(jobId);
        if (detail == null) {
            throw new BusinessException(404, "AI 任务不存在");
        }
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        emittersByJob.computeIfAbsent(jobId, ignored -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(jobId, emitter));
        emitter.onTimeout(() -> {
            removeEmitter(jobId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> removeEmitter(jobId, emitter));
        send(emitter, snapshot(task, detail, "snapshot"));
        if (isTerminal(task.getTaskStatus())) {
            emitter.complete();
            removeEmitter(jobId, emitter);
        }
        return emitter;
    }

    @Override
    public void publishStage(Long jobId, String stage, Integer progress) {
        if (!isValidStage(stage) || progress == null || progress < 0 || progress > 100) {
            return;
        }
        BizAnalysisTask task = taskMapper.selectById(jobId);
        BizAiExecutionDetail detail = executionDetailMapper.selectById(jobId);
        if (task == null || detail == null || isTerminal(task.getTaskStatus())
                || progress < defaultProgress(task.getProgress()) || isStaleStage(detail.getJobStage(), stage)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        task.setProgress(progress);
        task.setUpdateTime(now);
        detail.setJobStage(stage);
        detail.setVersion(defaultVersion(detail.getVersion()) + 1);
        detail.setUpdateTime(now);
        taskMapper.updateById(task);
        executionDetailMapper.updateById(detail);
        broadcast(jobId, snapshot(task, detail, "stage"));
    }

    @Override
    public void publishTerminal(Long jobId) {
        BizAnalysisTask task = taskMapper.selectById(jobId);
        BizAiExecutionDetail detail = executionDetailMapper.selectById(jobId);
        if (task == null || detail == null || !isTerminal(task.getTaskStatus())) {
            return;
        }
        String eventType = "success".equals(task.getTaskStatus()) ? "completed" : task.getTaskStatus();
        broadcast(jobId, snapshot(task, detail, eventType));
        completeJobEmitters(jobId);
    }

    private BizAnalysisTask requireOwnedTask(Long jobId) {
        Long userId = BaseContext.getCurrentId();
        BizAnalysisTask task = jobId == null ? null : taskMapper.selectById(jobId);
        if (userId == null || task == null || task.getUserId() == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(404, "AI 任务不存在");
        }
        return task;
    }

    private void broadcast(Long jobId, AiJobEventVO event) {
        Set<SseEmitter> emitters = emittersByJob.get(jobId);
        if (emitters == null) {
            return;
        }
        emitters.forEach(emitter -> send(emitter, event));
    }

    private void send(SseEmitter emitter, AiJobEventVO event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(event.eventId()))
                    .name(event.eventType())
                    .data(event));
        } catch (IOException | IllegalStateException exception) {
            emitter.complete();
        }
    }

    private void completeJobEmitters(Long jobId) {
        Set<SseEmitter> emitters = emittersByJob.remove(jobId);
        if (emitters != null) {
            emitters.forEach(SseEmitter::complete);
        }
    }

    private void removeEmitter(Long jobId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByJob.get(jobId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByJob.remove(jobId, emitters);
            }
        }
    }

    private AiJobEventVO snapshot(BizAnalysisTask task, BizAiExecutionDetail detail, String eventType) {
        return new AiJobEventVO(
                (long) defaultVersion(detail.getVersion()),
                eventType,
                task.getId(),
                task.getTaskType(),
                task.getTaskStatus(),
                detail.getJobStage(),
                task.getProgress(),
                detail.getResultType(),
                detail.getResultId(),
                task.getUpdateTime() == null ? LocalDateTime.now() : task.getUpdateTime());
    }

    private boolean isValidStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return false;
        }
        try {
            AiJobStageEnum.valueOf(stage.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isStaleStage(String existingStage, String newStage) {
        if (existingStage == null || existingStage.isBlank()) {
            return false;
        }
        return AiJobStageEnum.valueOf(existingStage.toUpperCase(Locale.ROOT)).ordinal()
                > AiJobStageEnum.valueOf(newStage.toUpperCase(Locale.ROOT)).ordinal();
    }

    private boolean isTerminal(String status) {
        return "success".equals(status) || "failed".equals(status) || "timed_out".equals(status)
                || "cancelled".equals(status);
    }

    private int defaultProgress(Integer progress) {
        return progress == null ? 0 : progress;
    }

    private int defaultVersion(Integer version) {
        return version == null ? 0 : version;
    }
}
