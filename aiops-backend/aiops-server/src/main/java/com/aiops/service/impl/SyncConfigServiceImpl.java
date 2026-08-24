package com.aiops.service.impl;

import com.aiops.dto.CrawlerImportDTO;
import com.aiops.dto.CsvImportDTO;
import com.aiops.dto.SyncConfigDTO;
import com.aiops.entity.BizSyncConfig;
import com.aiops.entity.BizSyncExecution;
import com.aiops.entity.BizTaskRecord;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizSyncConfigMapper;
import com.aiops.mapper.BizSyncExecutionMapper;
import com.aiops.mapper.BizTaskRecordMapper;
import com.aiops.result.PageResult;
import com.aiops.service.DataImportService;
import com.aiops.service.QuartzScheduleService;
import com.aiops.service.SyncConfigService;
import com.aiops.vo.SyncConfigVO;
import com.aiops.vo.SyncExecutionVO;
import com.aiops.vo.TaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncConfigServiceImpl implements SyncConfigService {

    private static final String DEFAULT_CRON = "0 0 2 * * ?";

    private final BizSyncConfigMapper syncConfigMapper;
    private final BizSyncExecutionMapper syncExecutionMapper;
    private final BizTaskRecordMapper taskRecordMapper;
    private final DataImportService dataImportService;
    private final QuartzScheduleService quartzScheduleService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<SyncConfigVO> pageConfigs(Integer pageNum, Integer pageSize, String sourceType, Integer enabled) {
        Page<BizSyncConfig> page = syncConfigMapper.selectPage(new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize)),
                new LambdaQueryWrapper<BizSyncConfig>()
                        .eq(hasText(sourceType), BizSyncConfig::getSourceType, sourceType)
                        .eq(enabled != null, BizSyncConfig::getEnabled, enabled)
                        .orderByDesc(BizSyncConfig::getCreateTime));
        return PageResult.of(page.getRecords().stream().map(this::toConfigVO).toList(),
                page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public SyncConfigVO createConfig(SyncConfigDTO syncConfigDTO) {
        validate(syncConfigDTO);
        BizSyncConfig config = new BizSyncConfig();
        copyToEntity(syncConfigDTO, config);
        config.setNextRunTime(nextRunTime(config.getCronExpression()));
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        syncConfigMapper.insert(config);
        if (Integer.valueOf(1).equals(config.getEnabled())) {
            quartzScheduleService.scheduleSyncConfig(config);
        }
        return toConfigVO(config);
    }

    @Override
    public SyncConfigVO updateConfig(Long configId, SyncConfigDTO syncConfigDTO) {
        BizSyncConfig config = requireConfig(configId);
        validate(syncConfigDTO);
        copyToEntity(syncConfigDTO, config);
        config.setNextRunTime(nextRunTime(config.getCronExpression()));
        config.setUpdateTime(LocalDateTime.now());
        syncConfigMapper.updateById(config);
        if (Integer.valueOf(1).equals(config.getEnabled())) {
            quartzScheduleService.scheduleSyncConfig(config);
        } else {
            quartzScheduleService.removeSyncConfig(config.getId());
        }
        return toConfigVO(config);
    }

    @Override
    public SyncConfigVO enableConfig(Long configId) {
        BizSyncConfig config = requireConfig(configId);
        config.setEnabled(1);
        config.setNextRunTime(nextRunTime(config.getCronExpression()));
        config.setUpdateTime(LocalDateTime.now());
        syncConfigMapper.updateById(config);
        quartzScheduleService.scheduleSyncConfig(config);
        return toConfigVO(config);
    }

    @Override
    public SyncConfigVO disableConfig(Long configId) {
        BizSyncConfig config = requireConfig(configId);
        config.setEnabled(0);
        config.setNextRunTime(null);
        config.setUpdateTime(LocalDateTime.now());
        syncConfigMapper.updateById(config);
        quartzScheduleService.removeSyncConfig(configId);
        return toConfigVO(config);
    }

    @Override
    public SyncExecutionVO triggerNow(Long configId) {
        return executeSyncConfig(configId, "manual");
    }

    @Override
    public SyncExecutionVO executeSyncConfig(Long configId, String triggerType) {
        BizSyncConfig config = requireConfig(configId);
        BizSyncExecution execution = new BizSyncExecution();
        execution.setConfigId(configId);
        execution.setTriggerType(hasText(triggerType) ? triggerType : "scheduled");
        execution.setExecutionStatus("processing");
        execution.setStartTime(LocalDateTime.now());
        execution.setCreateTime(LocalDateTime.now());
        execution.setUpdateTime(LocalDateTime.now());
        syncExecutionMapper.insert(execution);

        BizTaskRecord taskRecord = createTaskRecord(config, execution);
        try {
            TaskVO linkedTask = executeImport(config);
            execution.setLinkedTaskId(linkedTask.getTaskId());
            execution.setLinkedTaskType(resolveLinkedTaskType(config));
            execution.setExecutionStatus("success");
            taskRecord.setTaskStatus("success");
            taskRecord.setProgress(100);
            taskRecord.setSourceId(execution.getId());
            taskRecord.setEndTime(LocalDateTime.now());
        } catch (Exception exception) {
            execution.setExecutionStatus("failed");
            execution.setErrorMessage(exception.getMessage());
            taskRecord.setTaskStatus("failed");
            taskRecord.setProgress(100);
            taskRecord.setErrorMessage(exception.getMessage());
            taskRecord.setEndTime(LocalDateTime.now());
        }
        execution.setEndTime(LocalDateTime.now());
        execution.setUpdateTime(LocalDateTime.now());
        syncExecutionMapper.updateById(execution);
        taskRecord.setUpdateTime(LocalDateTime.now());
        taskRecordMapper.updateById(taskRecord);

        config.setLastRunTime(execution.getStartTime());
        config.setNextRunTime(Integer.valueOf(1).equals(config.getEnabled()) ? nextRunTime(config.getCronExpression()) : null);
        config.setUpdateTime(LocalDateTime.now());
        syncConfigMapper.updateById(config);
        return toExecutionVO(execution, config.getSyncName());
    }

    @Override
    public PageResult<SyncExecutionVO> pageExecutions(Integer pageNum, Integer pageSize, Long configId, String status) {
        Page<BizSyncExecution> page = syncExecutionMapper.selectPage(new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize)),
                new LambdaQueryWrapper<BizSyncExecution>()
                        .eq(configId != null, BizSyncExecution::getConfigId, configId)
                        .eq(hasText(status), BizSyncExecution::getExecutionStatus, status)
                        .orderByDesc(BizSyncExecution::getCreateTime));
        List<SyncExecutionVO> records = page.getRecords().stream()
                .map(execution -> {
                    BizSyncConfig config = syncConfigMapper.selectById(execution.getConfigId());
                    return toExecutionVO(execution, config == null ? null : config.getSyncName());
                })
                .toList();
        return PageResult.of(records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    private TaskVO executeImport(BizSyncConfig config) {
        if ("crawler".equals(config.getSourceType())) {
            CrawlerImportDTO dto = new CrawlerImportDTO();
            dto.setPlatform(config.getPlatform());
            dto.setTargetUrl(config.getTargetUrl());
            dto.setTargetType(config.getTargetType());
            dto.setMaxCount(config.getMaxCount());
            dto.setDelaySeconds(config.getDelaySeconds());
            return dataImportService.importByCrawler(dto);
        }
        CsvImportDTO dto = new CsvImportDTO();
        dto.setFileId(config.getFileId());
        dto.setObjectKey(config.getObjectKey());
        dto.setFileUrl(config.getFileUrl());
        dto.setDataPath(config.getDataPath());
        dto.setDataSource(config.getDataSource());
        dto.setImportMode(config.getImportMode());
        return dataImportService.importCsv(dto);
    }

    private BizTaskRecord createTaskRecord(BizSyncConfig config, BizSyncExecution execution) {
        BizTaskRecord record = new BizTaskRecord();
        record.setTaskName("同步：" + config.getSyncName());
        record.setTaskType("scheduled_sync");
        record.setTaskStatus("processing");
        record.setProgress(0);
        record.setSourceTable("biz_sync_execution");
        record.setSourceId(execution.getId());
        record.setRequestParam(toJson(config));
        record.setStartTime(execution.getStartTime());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        taskRecordMapper.insert(record);
        return record;
    }

    private void validate(SyncConfigDTO dto) {
        if (dto == null) {
            throw new BusinessException(400, "同步配置不能为空");
        }
        dto.setSyncName(defaultIfBlank(dto.getSyncName(), "未命名同步任务"));
        dto.setSourceType(defaultIfBlank(dto.getSourceType(), "olist_directory"));
        dto.setDataSource(defaultIfBlank(dto.getDataSource(), "olist"));
        dto.setImportMode(defaultIfBlank(dto.getImportMode(), "incremental"));
        dto.setCronExpression(defaultIfBlank(dto.getCronExpression(), DEFAULT_CRON));
        dto.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        dto.setAutoAnalysis(dto.getAutoAnalysis() == null ? 0 : dto.getAutoAnalysis());
        if (!CronExpression.isValidExpression(dto.getCronExpression())) {
            throw new BusinessException(400, "Cron 表达式不合法");
        }
        if ("olist_directory".equals(dto.getSourceType()) && !hasText(dto.getDataPath())) {
            throw new BusinessException(400, "Olist 目录同步需要填写本地数据目录");
        }
        if ("csv_file".equals(dto.getSourceType()) && dto.getFileId() == null && !hasText(dto.getFileUrl())) {
            throw new BusinessException(400, "单 CSV 同步需要上传文件或填写文件 URL");
        }
        if ("crawler".equals(dto.getSourceType()) && (!hasText(dto.getPlatform()) || !hasText(dto.getTargetUrl()))) {
            throw new BusinessException(400, "爬虫同步需要填写平台和目标 URL");
        }
    }

    private void copyToEntity(SyncConfigDTO dto, BizSyncConfig config) {
        config.setSyncName(dto.getSyncName());
        config.setSourceType(dto.getSourceType());
        config.setDataSource(dto.getDataSource());
        config.setImportMode(dto.getImportMode());
        config.setDataPath(dto.getDataPath());
        config.setFileId(dto.getFileId());
        config.setObjectKey(dto.getObjectKey());
        config.setFileUrl(dto.getFileUrl());
        config.setPlatform(dto.getPlatform());
        config.setTargetUrl(dto.getTargetUrl());
        config.setTargetType(defaultIfBlank(dto.getTargetType(), "product_comment"));
        config.setMaxCount(dto.getMaxCount() == null ? 100 : dto.getMaxCount());
        config.setDelaySeconds(dto.getDelaySeconds() == null ? 3 : dto.getDelaySeconds());
        config.setCronExpression(dto.getCronExpression());
        config.setAutoAnalysis(dto.getAutoAnalysis());
        config.setEnabled(dto.getEnabled());
        config.setRemark(dto.getRemark());
    }

    private BizSyncConfig requireConfig(Long configId) {
        BizSyncConfig config = syncConfigMapper.selectById(configId);
        if (config == null) {
            throw new BusinessException(404, "同步配置不存在");
        }
        return config;
    }

    private SyncConfigVO toConfigVO(BizSyncConfig config) {
        return new SyncConfigVO(config.getId(), config.getSyncName(), config.getSourceType(), config.getDataSource(),
                config.getImportMode(), config.getDataPath(), config.getFileId(), config.getObjectKey(),
                config.getFileUrl(), config.getPlatform(), config.getTargetUrl(), config.getTargetType(),
                config.getMaxCount(), config.getDelaySeconds(), config.getCronExpression(),
                config.getAutoAnalysis(), config.getEnabled(), config.getRemark(), config.getLastRunTime(),
                config.getNextRunTime(), config.getCreateTime(), config.getUpdateTime());
    }

    private SyncExecutionVO toExecutionVO(BizSyncExecution execution, String syncName) {
        return new SyncExecutionVO(execution.getId(), execution.getConfigId(), syncName, execution.getTriggerType(),
                execution.getExecutionStatus(), execution.getLinkedTaskId(), execution.getLinkedTaskType(),
                execution.getErrorMessage(), execution.getStartTime(), execution.getEndTime(), execution.getCreateTime());
    }

    private String resolveLinkedTaskType(BizSyncConfig config) {
        return "crawler".equals(config.getSourceType()) ? "crawler_import" : "csv_import";
    }

    private LocalDateTime nextRunTime(String cronExpression) {
        try {
            Date next = new CronExpression(cronExpression).getNextValidTimeAfter(new Date());
            return next == null ? null : LocalDateTime.ofInstant(next.toInstant(), ZoneId.systemDefault());
        } catch (Exception exception) {
            return null;
        }
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
