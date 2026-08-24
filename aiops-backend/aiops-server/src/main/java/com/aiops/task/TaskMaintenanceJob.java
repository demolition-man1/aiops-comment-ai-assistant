package com.aiops.task;

import com.aiops.properties.TaskMaintenanceProperties;
import com.aiops.service.TaskMaintenanceService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.concurrent.TimeUnit;

@Slf4j
public class TaskMaintenanceJob extends QuartzJobBean {

    @Autowired
    private TaskMaintenanceService taskMaintenanceService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TaskMaintenanceProperties properties;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return;
        }
        RLock lock = redissonClient.getLock("aiops:lock:task-maintenance");
        boolean locked = false;
        try {
            locked = lock.tryLock(properties.getLockWaitSeconds(), properties.getLockLeaseSeconds(), TimeUnit.SECONDS);
            if (!locked) {
                return;
            }
            int recovered = taskMaintenanceService.failStaleProcessingTasks();
            if (recovered > 0) {
                log.info("Task maintenance marked {} stale tasks as failed", recovered);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException("Task maintenance interrupted", exception);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
