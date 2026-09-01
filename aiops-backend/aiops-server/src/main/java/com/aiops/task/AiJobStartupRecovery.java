package com.aiops.task;

import com.aiops.service.AiJobRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiJobStartupRecovery implements ApplicationListener<ApplicationReadyEvent> {

    private final AiJobRecoveryService recoveryService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        recoveryService.resubmitPendingJobs();
    }
}
