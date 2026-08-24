package com.aiops.runner;

import com.aiops.service.QuartzScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SyncScheduleStartupRunner implements ApplicationRunner {

    private final QuartzScheduleService quartzScheduleService;

    @Override
    public void run(ApplicationArguments args) {
        quartzScheduleService.rescheduleAllEnabled();
    }
}
