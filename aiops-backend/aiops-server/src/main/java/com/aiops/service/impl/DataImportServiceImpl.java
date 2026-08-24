package com.aiops.service.impl;

import com.aiops.client.PythonAnalysisClient;
import com.aiops.constant.RedisKeyConstant;
import com.aiops.dto.CrawlerImportDTO;
import com.aiops.dto.CsvImportDTO;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.entity.BizCrawlTask;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.mapper.BizCrawlTaskMapper;
import com.aiops.service.CacheService;
import com.aiops.service.DataImportService;
import com.aiops.service.FileService;
import com.aiops.vo.FileSignedUrlVO;
import com.aiops.vo.TaskVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataImportServiceImpl implements DataImportService {

    private final BizAnalysisTaskMapper analysisTaskMapper;
    private final BizCrawlTaskMapper crawlTaskMapper;
    private final PythonAnalysisClient pythonAnalysisClient;
    private final CacheService cacheService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;

    @Override
    public TaskVO importCsv(CsvImportDTO csvImportDTO) {
        validateCsvImport(csvImportDTO);
        BizAnalysisTask task = new BizAnalysisTask();
        task.setTaskType("csv_import");
        task.setTaskStatus("processing");
        task.setProgress(0);
        task.setRequestParam(toJson(csvImportDTO));
        task.setStartTime(LocalDateTime.now());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        analysisTaskMapper.insert(task);
        cacheTask(task.getId(), "csv", "processing", 0);

        String objectKey = csvImportDTO.getObjectKey();
        String fileUrl = csvImportDTO.getFileUrl();
        if (csvImportDTO.getFileId() != null) {
            FileSignedUrlVO signedUrl = fileService.signedUrl(csvImportDTO.getFileId());
            fileUrl = signedUrl.getSignedUrl();
            objectKey = hasText(objectKey) ? objectKey : signedUrl.getObjectKey();
        }

        Map<String, Object> request = new HashMap<>();
        request.put("taskId", task.getId());
        request.put("fileId", csvImportDTO.getFileId());
        request.put("objectKey", objectKey);
        request.put("fileUrl", fileUrl);
        request.put("dataPath", csvImportDTO.getDataPath());
        request.put("dataSource", csvImportDTO.getDataSource());
        request.put("importMode", csvImportDTO.getImportMode());
        try {
            taskExecutor.execute(() -> callPythonImport(task, request));
        } catch (RuntimeException exception) {
            task.setTaskStatus("failed");
            task.setErrorMessage(exception.getMessage());
            task.setEndTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            analysisTaskMapper.updateById(task);
            cacheTask(task.getId(), "csv", task.getTaskStatus(), task.getProgress());
        }
        return new TaskVO(task.getId(), task.getTaskStatus(), "csv", task.getProgress(), null, null, null, task.getErrorMessage());
    }

    @Override
    public TaskVO importByCrawler(CrawlerImportDTO crawlerImportDTO) {
        validateCrawlerImport(crawlerImportDTO);
        BizCrawlTask task = new BizCrawlTask();
        task.setPlatform(crawlerImportDTO.getPlatform());
        task.setTargetUrl(crawlerImportDTO.getTargetUrl());
        task.setTargetType(crawlerImportDTO.getTargetType());
        task.setTaskStatus("processing");
        task.setProgress(0);
        task.setMaxCount(crawlerImportDTO.getMaxCount());
        task.setDelaySeconds(crawlerImportDTO.getDelaySeconds());
        task.setStartTime(LocalDateTime.now());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        crawlTaskMapper.insert(task);
        cacheTask(task.getId(), "crawler", "processing", 0);

        Map<String, Object> request = new HashMap<>();
        request.put("taskId", task.getId());
        request.put("platform", crawlerImportDTO.getPlatform());
        request.put("targetUrl", crawlerImportDTO.getTargetUrl());
        request.put("targetType", crawlerImportDTO.getTargetType());
        request.put("maxCount", crawlerImportDTO.getMaxCount());
        request.put("delaySeconds", crawlerImportDTO.getDelaySeconds());
        try {
            Map<String, Object> response = pythonAnalysisClient.importByCrawler(request);
            boolean success = Boolean.TRUE.equals(response == null ? null : response.get("success"));
            task.setTaskStatus(success ? "success" : "failed");
            task.setProgress(intValue(response, "progress", 100));
            task.setSuccessCount(intValue(response, "successCount", success ? 1 : 0));
            task.setFailCount(intValue(response, "failCount", success ? 0 : 1));
            if (!success) {
                task.setErrorMessage(stringValue(response, "message", "爬虫适配器暂不可用"));
            }
        } catch (Exception exception) {
            task.setTaskStatus("failed");
            task.setProgress(100);
            task.setSuccessCount(0);
            task.setFailCount(1);
            task.setErrorMessage(exception.getMessage());
        }
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        crawlTaskMapper.updateById(task);
        cacheTask(task.getId(), "crawler", task.getTaskStatus(), task.getProgress());
        return new TaskVO(task.getId(), task.getTaskStatus(), "crawler", task.getProgress(), null,
                task.getSuccessCount(), task.getFailCount(), task.getErrorMessage());
    }

    @Override
    public TaskVO getImportTask(Long taskId) {
        return getImportTask(taskId, null);
    }

    @Override
    public TaskVO getImportTask(Long taskId, String importType) {
        String normalizedType = normalizeImportType(importType)
                .orElseGet(() -> cacheService.get(String.format(RedisKeyConstant.TASK_TYPE, taskId), String.class)
                        .flatMap(this::normalizeImportType)
                        .orElse(null));
        if ("crawler".equals(normalizedType)) {
            BizCrawlTask crawlTask = crawlTaskMapper.selectById(taskId);
            if (crawlTask != null) {
                cacheTask(taskId, "crawler", crawlTask.getTaskStatus(), crawlTask.getProgress());
                return new TaskVO(crawlTask.getId(), crawlTask.getTaskStatus(), "crawler", crawlTask.getProgress(),
                        null, crawlTask.getSuccessCount(), crawlTask.getFailCount(), crawlTask.getErrorMessage());
            }
        }
        if ("csv".equals(normalizedType)) {
            BizAnalysisTask analysisTask = analysisTaskMapper.selectById(taskId);
            if (analysisTask != null) {
                cacheTask(taskId, "csv", analysisTask.getTaskStatus(), analysisTask.getProgress());
                return new TaskVO(analysisTask.getId(), analysisTask.getTaskStatus(), "csv", analysisTask.getProgress(),
                        null, null, null, analysisTask.getErrorMessage());
            }
        }
        BizAnalysisTask analysisTask = analysisTaskMapper.selectById(taskId);
        if (analysisTask != null) {
            cacheTask(taskId, "csv", analysisTask.getTaskStatus(), analysisTask.getProgress());
            return new TaskVO(analysisTask.getId(), analysisTask.getTaskStatus(), "csv", analysisTask.getProgress(),
                    null, null, null, analysisTask.getErrorMessage());
        }
        BizCrawlTask crawlTask = crawlTaskMapper.selectById(taskId);
        if (crawlTask != null) {
            cacheTask(taskId, "crawler", crawlTask.getTaskStatus(), crawlTask.getProgress());
            return new TaskVO(crawlTask.getId(), crawlTask.getTaskStatus(), "crawler", crawlTask.getProgress(),
                    null, crawlTask.getSuccessCount(), crawlTask.getFailCount(), crawlTask.getErrorMessage());
        }
        throw new BusinessException(404, "导入任务不存在");
    }

    private void callPythonImport(BizAnalysisTask task, Map<String, Object> request) {
        try {
            Map<String, Object> response = pythonAnalysisClient.importCsv(request);
            boolean success = response == null || !Boolean.FALSE.equals(response.get("success"));
            task.setTaskStatus(success ? "success" : "failed");
            task.setProgress(intValue(response, "progress", 100));
            if (!success) {
                task.setErrorMessage(stringValue(response, "message", "CSV 导入失败"));
            }
        } catch (Exception exception) {
            task.setTaskStatus("failed");
            task.setProgress(0);
            task.setErrorMessage(exception.getMessage());
        }
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        analysisTaskMapper.updateById(task);
        cacheTask(task.getId(), "csv", task.getTaskStatus(), task.getProgress());
    }

    private void validateCsvImport(CsvImportDTO csvImportDTO) {
        if (csvImportDTO == null) {
            throw new BusinessException(400, "CSV 导入参数不能为空");
        }
        String dataSource = blankToDefault(csvImportDTO.getDataSource(), "olist");
        csvImportDTO.setDataSource(dataSource);
        csvImportDTO.setImportMode(blankToDefault(csvImportDTO.getImportMode(), "full"));
        boolean hasDataPath = hasText(csvImportDTO.getDataPath());
        boolean hasFile = csvImportDTO.getFileId() != null || hasText(csvImportDTO.getFileUrl());
        if (!hasDataPath && !hasFile) {
            throw new BusinessException(400, "本地 Olist 数据目录不能为空，或请先上传包含 product_id 和 review_score 的评论 CSV");
        }
    }

    private void validateCrawlerImport(CrawlerImportDTO crawlerImportDTO) {
        if (crawlerImportDTO == null) {
            throw new BusinessException(400, "爬虫导入参数不能为空");
        }
        if (!hasText(crawlerImportDTO.getPlatform())) {
            throw new BusinessException(400, "爬虫平台不能为空");
        }
        if (!hasText(crawlerImportDTO.getTargetUrl())) {
            throw new BusinessException(400, "目标 URL 不能为空");
        }
        if (crawlerImportDTO.getMaxCount() == null || crawlerImportDTO.getMaxCount() <= 0) {
            crawlerImportDTO.setMaxCount(100);
        }
        if (crawlerImportDTO.getDelaySeconds() == null || crawlerImportDTO.getDelaySeconds() <= 0) {
            crawlerImportDTO.setDelaySeconds(3);
        }
    }

    private void cacheTask(Long taskId, String type, String status, Integer progress) {
        cacheService.set(String.format(RedisKeyConstant.TASK_TYPE, taskId), type, Duration.ofHours(2));
        cacheService.set(String.format(RedisKeyConstant.TASK_STATUS, taskId), status, Duration.ofHours(2));
        cacheService.set(String.format(RedisKeyConstant.TASK_PROGRESS, taskId), progress, Duration.ofHours(2));
    }

    private java.util.Optional<String> normalizeImportType(String importType) {
        if (!hasText(importType)) {
            return java.util.Optional.empty();
        }
        String normalized = importType.trim().toLowerCase();
        if ("csv".equals(normalized) || "csv_import".equals(normalized)) {
            return java.util.Optional.of("csv");
        }
        if ("crawler".equals(normalized) || "crawl".equals(normalized)) {
            return java.util.Optional.of("crawler");
        }
        return java.util.Optional.empty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private Integer intValue(Map<String, Object> map, String key, Integer defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && hasText(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String stringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "导入任务参数序列化失败");
        }
    }
}
