package com.aiops.task;

import com.aiops.properties.AiJobProperties;
import com.aiops.service.AiJobRecoveryService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AiJobRecoveryJob extends QuartzJobBean {

    @Autowired
    private AiJobRecoveryService recoveryService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private AiJobProperties properties;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        if (!Boolean.TRUE.equals(properties.getRecoveryEnabled())) {
            return;
        }
        RLock lock = redissonClient.getLock("aiops:lock:ai-job-recovery");
        boolean locked = false;
        try {
            locked = lock.tryLock(0, Math.max(1, properties.getLeaseSeconds()), TimeUnit.SECONDS);
            if (locked) {
                int recovered = recoveryService.recoverJobs();
                if (recovered > 0) {
                    log.info("Recovered {} AI jobs", recovered);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
