package com.aiops.service;

import com.aiops.entity.BizSyncConfig;

public interface QuartzScheduleService {
    void scheduleSyncConfig(BizSyncConfig config);

    void removeSyncConfig(Long configId);

    void rescheduleAllEnabled();
}
