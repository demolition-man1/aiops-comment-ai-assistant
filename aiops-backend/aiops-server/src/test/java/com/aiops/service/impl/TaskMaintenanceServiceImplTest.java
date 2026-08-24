package com.aiops.service.impl;

import com.aiops.constant.RedisKeyConstant;
import com.aiops.entity.BizAnalysisTask;
import com.aiops.mapper.BizAnalysisTaskMapper;
import com.aiops.properties.TaskMaintenanceProperties;
import com.aiops.service.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskMaintenanceServiceImplTest {

    @Mock
    private BizAnalysisTaskMapper taskMapper;

    @Mock
    private CacheService cacheService;

    @Test
    void marksStaleProcessingAnalysisTasksAsFailedAndRefreshesCache() {
        TaskMaintenanceProperties properties = new TaskMaintenanceProperties();
        properties.setStaleProcessingMinutes(30);

        BizAnalysisTask staleTask = new BizAnalysisTask();
        staleTask.setId(33L);
        staleTask.setTaskStatus("processing");
        staleTask.setProgress(20);
        staleTask.setUpdateTime(LocalDateTime.now().minusHours(2));
        when(taskMapper.selectList(any())).thenReturn(List.of(staleTask));

        TaskMaintenanceServiceImpl service = new TaskMaintenanceServiceImpl(taskMapper, cacheService, properties);

        int recovered = service.failStaleProcessingTasks();

        assertThat(recovered).isEqualTo(1);
        ArgumentCaptor<BizAnalysisTask> taskCaptor = ArgumentCaptor.forClass(BizAnalysisTask.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        BizAnalysisTask updated = taskCaptor.getValue();
        assertThat(updated.getTaskStatus()).isEqualTo("failed");
        assertThat(updated.getProgress()).isEqualTo(100);
        assertThat(updated.getErrorMessage()).contains("定时任务检测");
        verify(cacheService).set(eq(String.format(RedisKeyConstant.TASK_STATUS, 33L)), eq("failed"), any(Duration.class));
        verify(cacheService).set(eq(String.format(RedisKeyConstant.TASK_PROGRESS, 33L)), eq(100), any(Duration.class));
    }
}
