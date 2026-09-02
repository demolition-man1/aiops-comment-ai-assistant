package com.aiops.mapper;

import com.aiops.entity.BizAiCallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BizAiCallLogMapper extends BaseMapper<BizAiCallLog> {

    @Update("""
            update biz_ai_call_log
            set queue_latency_ms = #{queueLatencyMs}, total_latency_ms = #{totalLatencyMs}, error_code = #{errorCode}
            where job_id = #{jobId}
            """)
    int updateJobObservability(@Param("jobId") Long jobId, @Param("queueLatencyMs") Long queueLatencyMs,
                               @Param("totalLatencyMs") Long totalLatencyMs, @Param("errorCode") String errorCode);
}
