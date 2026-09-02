package com.aiops.config;

import com.aiops.properties.AiJobProperties;
import com.aiops.properties.TaskMaintenanceProperties;
import com.aiops.task.AiJobRecoveryJob;
import com.aiops.task.TaskMaintenanceJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail aiJobRecoveryJobDetail() {
        return JobBuilder.newJob(AiJobRecoveryJob.class)
                .withIdentity("aiJobRecoveryJob", "aiops")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger aiJobRecoveryTrigger(JobDetail aiJobRecoveryJobDetail, AiJobProperties properties) {
        return TriggerBuilder.newTrigger()
                .forJob(aiJobRecoveryJobDetail)
                .withIdentity("aiJobRecoveryTrigger", "aiops")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(Math.max(30, properties.getRecoveryIntervalSeconds()))
                        .repeatForever())
                .build();
    }

    @Bean
    public JobDetail taskMaintenanceJobDetail() {
        return JobBuilder.newJob(TaskMaintenanceJob.class)
                .withIdentity("taskMaintenanceJob", "aiops")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger taskMaintenanceTrigger(JobDetail taskMaintenanceJobDetail,
                                          TaskMaintenanceProperties properties) {
        return TriggerBuilder.newTrigger()
                .forJob(taskMaintenanceJobDetail)
                .withIdentity("taskMaintenanceTrigger", "aiops")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(Math.max(30, properties.getIntervalSeconds()))
                        .repeatForever())
                .build();
    }
}
