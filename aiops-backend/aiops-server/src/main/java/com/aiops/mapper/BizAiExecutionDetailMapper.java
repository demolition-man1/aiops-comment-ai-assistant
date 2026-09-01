package com.aiops.mapper;

import com.aiops.entity.BizAiExecutionDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface BizAiExecutionDetailMapper extends BaseMapper<BizAiExecutionDetail> {

    @Select("select * from biz_ai_execution_detail where task_id = #{taskId} for update")
    BizAiExecutionDetail selectByIdForUpdate(@Param("taskId") Long taskId);

    @Update("""
            update biz_ai_execution_detail
            set lease_owner = #{leaseOwner}, lease_until = #{leaseUntil}, version = version + 1,
                update_time = #{now}
            where task_id = #{taskId}
              and version = #{version}
              and (lease_until is null or lease_until < #{now})
            """)
    int claimLease(@Param("taskId") Long taskId, @Param("version") Integer version,
                   @Param("leaseOwner") String leaseOwner, @Param("leaseUntil") LocalDateTime leaseUntil,
                   @Param("now") LocalDateTime now);
}
