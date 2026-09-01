package com.aiops.mapper;

import com.aiops.entity.BizAnalysisTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface BizAnalysisTaskMapper extends BaseMapper<BizAnalysisTask> {

    @Update("""
            update biz_analysis_task
            set task_status = 'processing', start_time = #{startTime}, update_time = #{startTime}
            where id = #{taskId} and task_status = 'pending'
            """)
    int markAiJobProcessing(@Param("taskId") Long taskId, @Param("startTime") LocalDateTime startTime);
}

