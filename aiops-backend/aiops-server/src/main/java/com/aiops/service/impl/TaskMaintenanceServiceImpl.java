package com.aiops.service.impl;

import com.aiops.constant.RedisKeyConstant;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.properties.TaskMaintenanceProperties;
import com.aiops.service.CacheService;
import com.aiops.service.TaskMaintenanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskMaintenanceServiceImpl implements TaskMaintenanceService {

    private final BizAnalysisTaskMapper taskMapper;
    private final CacheService cacheService;
    private final TaskMaintenanceProperties properties;

    @Override
    public int failStaleProcessingTasks() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(Math.max(1, properties.getStaleProcessingMinutes()));
        List<BizAnalysisTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<BizAnalysisTask>()
                .eq(BizAnalysisTask::getTaskStatus, "processing")
                .lt(BizAnalysisTask::getUpdateTime, cutoff));
        for (BizAnalysisTask task : tasks) {
            task.setTaskStatus("failed");
            task.setProgress(100);
            task.setErrorMessage("定时任务检测到任务长时间处于处理中，已自动标记失败");
            task.setEndTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
            cacheService.set(String.format(RedisKeyConstant.TASK_STATUS, task.getId()), "failed", Duration.ofHours(2));
            cacheService.set(String.format(RedisKeyConstant.TASK_PROGRESS, task.getId()), 100, Duration.ofHours(2));
        }
        return tasks.size();
    }
}
