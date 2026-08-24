package com.aiops.service.impl;

import com.aiops.dto.AnalysisTaskCreateDTO;
import com.aiops.dto.CrawlerImportDTO;
import com.aiops.dto.CsvImportDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.entity.BizCrawlTask;
import com.aiops.entity.BizSyncConfig;
import com.aiops.entity.BizSyncExecution;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCrawlTaskMapper;
import com.aiops.mapper.BizSyncConfigMapper;
import com.aiops.mapper.BizSyncExecutionMapper;
import com.aiops.result.PageResult;
import com.aiops.service.AnalysisService;
import com.aiops.service.DataImportService;
import com.aiops.service.SyncConfigService;
import com.aiops.service.TaskCenterService;
import com.aiops.vo.SyncExecutionVO;
import com.aiops.vo.TaskRecordVO;
import com.aiops.vo.TaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TaskCenterServiceImpl implements TaskCenterService {

    private static final String ANALYSIS_PREFIX = "analysis:";
    private static final String CRAWLER_PREFIX = "crawler:";
    private static final String SYNC_PREFIX = "sync:";

    private final BizAnalysisTaskMapper analysisTaskMapper;
    private final BizCrawlTaskMapper crawlTaskMapper;
    private final BizSyncExecutionMapper syncExecutionMapper;
    private final BizSyncConfigMapper syncConfigMapper;
    private final DataImportService dataImportService;
    private final AnalysisService analysisService;
    private final SyncConfigService syncConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<TaskRecordVO> pageTasks(Integer pageNum, Integer pageSize, String taskType,
                                              String taskStatus, String keyword) {
        List<TaskRecordVO> filtered = filteredTasks(taskType, taskStatus, keyword);
        int currentPage = normalizePageNum(pageNum);
        int currentSize = normalizePageSize(pageSize);
        int fromIndex = Math.min((currentPage - 1) * currentSize, filtered.size());
        int toIndex = Math.min(fromIndex + currentSize, filtered.size());
        return PageResult.of(filtered.subList(fromIndex, toIndex), filtered.size(), currentPage, currentSize);
    }

    @Override
    public TaskRecordVO getTask(String recordKey) {
        if (recordKey == null || recordKey.isBlank()) {
            throw new BusinessException(400, "任务标识不能为空");
        }
        if (recordKey.startsWith(ANALYSIS_PREFIX)) {
            BizAnalysisTask task = analysisTaskMapper.selectById(parseId(recordKey, ANALYSIS_PREFIX));
            if (task == null) {
                throw new BusinessException(404, "分析任务不存在");
            }
            return toAnalysisRecord(task);
        }
        if (recordKey.startsWith(CRAWLER_PREFIX)) {
            BizCrawlTask task = crawlTaskMapper.selectById(parseId(recordKey, CRAWLER_PREFIX));
            if (task == null) {
                throw new BusinessException(404, "爬虫任务不存在");
            }
            return toCrawlerRecord(task);
        }
        if (recordKey.startsWith(SYNC_PREFIX)) {
            BizSyncExecution task = syncExecutionMapper.selectById(parseId(recordKey, SYNC_PREFIX));
            if (task == null) {
                throw new BusinessException(404, "同步任务不存在");
            }
            return toSyncRecord(task);
        }
        throw new BusinessException(400, "任务标识格式错误");
    }

    @Override
    public TaskVO retryTask(String recordKey) {
        if (recordKey == null || recordKey.isBlank()) {
            throw new BusinessException(400, "任务标识不能为空");
        }
        if (recordKey.startsWith(ANALYSIS_PREFIX)) {
            return retryAnalysisTask(parseId(recordKey, ANALYSIS_PREFIX));
        }
        if (recordKey.startsWith(CRAWLER_PREFIX)) {
            return retryCrawlerTask(parseId(recordKey, CRAWLER_PREFIX));
        }
        if (recordKey.startsWith(SYNC_PREFIX)) {
            SyncExecutionVO execution = syncConfigService.triggerNow(resolveSyncConfigId(parseId(recordKey, SYNC_PREFIX)));
            return new TaskVO(execution.getId(), execution.getExecutionStatus(), "scheduled_sync",
                    "success".equals(execution.getExecutionStatus()) ? 100 : 0, 0, 0, 0,
                    execution.getErrorMessage());
        }
        throw new BusinessException(400, "任务标识格式错误");
    }

    @Override
    public byte[] exportTasksCsv(String taskType, String taskStatus, String keyword) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "recordKey", "taskName", "taskType", "taskStatus", "progress", "targetType",
                "targetId", "sourceTable", "errorMessage", "startTime", "endTime", "createTime");
        for (TaskRecordVO task : filteredTasks(taskType, taskStatus, keyword)) {
            appendLine(builder, task.getRecordKey(), task.getTaskName(), task.getTaskType(), task.getTaskStatus(),
                    task.getProgress(), task.getTargetType(), task.getTargetId(), task.getSourceTable(),
                    task.getErrorMessage(), task.getStartTime(), task.getEndTime(), task.getCreateTime());
        }
        return ("\uFEFF" + builder).getBytes(StandardCharsets.UTF_8);
    }

    private TaskVO retryAnalysisTask(Long taskId) {
        BizAnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "分析任务不存在");
        }
        if ("csv_import".equals(task.getTaskType())) {
            CsvImportDTO dto = parseJson(task.getRequestParam(), CsvImportDTO.class, "CSV 导入任务参数不可重试");
            return dataImportService.importCsv(dto);
        }
        AnalysisTaskCreateDTO dto = new AnalysisTaskCreateDTO();
        dto.setTargetType(task.getTargetType());
        dto.setTargetId(task.getTargetId());
        dto.setAnalysisType(task.getTaskType());
        return analysisService.createAnalysisTask(dto);
    }

    private TaskVO retryCrawlerTask(Long taskId) {
        BizCrawlTask task = crawlTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "爬虫任务不存在");
        }
        CrawlerImportDTO dto = new CrawlerImportDTO();
        dto.setPlatform(task.getPlatform());
        dto.setTargetUrl(task.getTargetUrl());
        dto.setTargetType(task.getTargetType());
        dto.setMaxCount(task.getMaxCount());
        dto.setDelaySeconds(task.getDelaySeconds());
        return dataImportService.importByCrawler(dto);
    }

    private Long resolveSyncConfigId(Long executionId) {
        BizSyncExecution execution = syncExecutionMapper.selectById(executionId);
        if (execution == null) {
            throw new BusinessException(404, "同步执行记录不存在");
        }
        return execution.getConfigId();
    }

    private List<TaskRecordVO> filteredTasks(String taskType, String taskStatus, String keyword) {
        return collectTaskRecords().stream()
                .filter(task -> !hasText(taskType) || taskType.equals(task.getTaskType()))
                .filter(task -> !hasText(taskStatus) || taskStatus.equals(task.getTaskStatus()))
                .filter(task -> !hasText(keyword) || containsKeyword(task, keyword.trim()))
                .sorted(Comparator.comparing(TaskRecordVO::getCreateTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .toList();
    }

    private List<TaskRecordVO> collectTaskRecords() {
        List<TaskRecordVO> tasks = new ArrayList<>();
        analysisTaskMapper.selectList(new LambdaQueryWrapper<BizAnalysisTask>().orderByDesc(BizAnalysisTask::getCreateTime))
                .forEach(task -> tasks.add(toAnalysisRecord(task)));
        crawlTaskMapper.selectList(new LambdaQueryWrapper<BizCrawlTask>().orderByDesc(BizCrawlTask::getCreateTime))
                .forEach(task -> tasks.add(toCrawlerRecord(task)));
        syncExecutionMapper.selectList(new LambdaQueryWrapper<BizSyncExecution>().orderByDesc(BizSyncExecution::getCreateTime))
                .forEach(task -> tasks.add(toSyncRecord(task)));
        return tasks;
    }

    private TaskRecordVO toAnalysisRecord(BizAnalysisTask task) {
        return new TaskRecordVO(ANALYSIS_PREFIX + task.getId(), task.getId(), "biz_analysis_task",
                taskName(task.getTaskType()), task.getTaskType(), task.getTaskStatus(), task.getProgress(),
                task.getTargetType(), task.getTargetId(), task.getErrorMessage(), task.getStartTime(),
                task.getEndTime(), task.getCreateTime());
    }

    private TaskRecordVO toCrawlerRecord(BizCrawlTask task) {
        return new TaskRecordVO(CRAWLER_PREFIX + task.getId(), task.getId(), "biz_crawl_task",
                "爬虫导入：" + defaultIfBlank(task.getPlatform(), "公开样例"), "crawler_import",
                task.getTaskStatus(), task.getProgress(), task.getTargetType(), task.getTargetUrl(),
                task.getErrorMessage(), task.getStartTime(), task.getEndTime(), task.getCreateTime());
    }

    private TaskRecordVO toSyncRecord(BizSyncExecution execution) {
        BizSyncConfig config = syncConfigMapper.selectById(execution.getConfigId());
        String syncName = config == null ? "同步任务" : config.getSyncName();
        return new TaskRecordVO(SYNC_PREFIX + execution.getId(), execution.getId(), "biz_sync_execution",
                "定时同步：" + defaultIfBlank(syncName, "未命名"), "scheduled_sync",
                execution.getExecutionStatus(), progressOf(execution.getExecutionStatus()),
                config == null ? null : config.getSourceType(), config == null ? null : syncName,
                execution.getErrorMessage(), execution.getStartTime(), execution.getEndTime(),
                execution.getCreateTime());
    }

    private String taskName(String taskType) {
        if ("csv_import".equals(taskType)) {
            return "CSV 导入任务";
        }
        if ("comment_analysis".equals(taskType)) {
            return "评论分析任务";
        }
        return defaultIfBlank(taskType, "后台任务");
    }

    private Integer progressOf(String status) {
        if ("success".equals(status) || "failed".equals(status)) {
            return 100;
        }
        return 0;
    }

    private boolean containsKeyword(TaskRecordVO task, String keyword) {
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return contains(task.getTaskName(), lowerKeyword)
                || contains(task.getTaskType(), lowerKeyword)
                || contains(task.getTaskStatus(), lowerKeyword)
                || contains(task.getTargetId(), lowerKeyword)
                || contains(task.getErrorMessage(), lowerKeyword);
    }

    private boolean contains(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private <T> T parseJson(String json, Class<T> valueType, String errorMessage) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (Exception exception) {
            throw new BusinessException(400, errorMessage);
        }
    }

    private Long parseId(String recordKey, String prefix) {
        try {
            return Long.parseLong(recordKey.substring(prefix.length()));
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, "任务标识格式错误");
        }
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void appendLine(StringBuilder builder, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(csvValue(values[index]));
        }
        builder.append('\n');
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
