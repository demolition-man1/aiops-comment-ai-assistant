package com.aiops.task;

import com.aiops.service.SyncConfigService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class SyncImportJob extends QuartzJobBean {

    @Autowired
    private SyncConfigService syncConfigService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long configId = context.getMergedJobDataMap().getLong("configId");
        syncConfigService.executeSyncConfig(configId, "scheduled");
    }
}
