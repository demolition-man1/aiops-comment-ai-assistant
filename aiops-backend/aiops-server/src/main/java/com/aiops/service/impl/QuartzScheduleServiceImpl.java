package com.aiops.service.impl;

import com.aiops.entity.BizSyncConfig;
import com.aiops.mapper.BizSyncConfigMapper;
import com.aiops.service.QuartzScheduleService;
import com.aiops.task.SyncImportJob;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuartzScheduleServiceImpl implements QuartzScheduleService {

    private static final String GROUP = "sync";

    private final Scheduler scheduler;
    private final BizSyncConfigMapper syncConfigMapper;

    @Override
    public void scheduleSyncConfig(BizSyncConfig config) {
        if (config == null || config.getId() == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            return;
        }
        try {
            removeSyncConfig(config.getId());
            JobDetail jobDetail = JobBuilder.newJob(SyncImportJob.class)
                    .withIdentity(jobKey(config.getId()))
                    .usingJobData("configId", config.getId())
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(config.getId()))
                    .forJob(jobDetail)
                    .withSchedule(CronScheduleBuilder.cronSchedule(config.getCronExpression())
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException exception) {
            throw new IllegalStateException("同步任务注册失败：" + exception.getMessage(), exception);
        }
    }

    @Override
    public void removeSyncConfig(Long configId) {
        if (configId == null) {
            return;
        }
        try {
            scheduler.deleteJob(jobKey(configId));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("同步任务移除失败：" + exception.getMessage(), exception);
        }
    }

    @Override
    public void rescheduleAllEnabled() {
        List<BizSyncConfig> configs = syncConfigMapper.selectList(new LambdaQueryWrapper<BizSyncConfig>()
                .eq(BizSyncConfig::getEnabled, 1));
        for (BizSyncConfig config : configs) {
            try {
                scheduleSyncConfig(config);
            } catch (RuntimeException exception) {
                log.warn("Failed to reschedule sync config {}: {}", config.getId(), exception.getMessage());
            }
        }
    }

    private JobKey jobKey(Long configId) {
        return JobKey.jobKey("syncImportJob-" + configId, GROUP);
    }

    private TriggerKey triggerKey(Long configId) {
        return TriggerKey.triggerKey("syncImportTrigger-" + configId, GROUP);
    }
}
