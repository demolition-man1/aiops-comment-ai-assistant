package com.aiops.mapper;

import com.aiops.entity.BizOperationReportEvidence;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BizOperationReportEvidenceMapper extends BaseMapper<BizOperationReportEvidence> {

    @Select("select * from biz_operation_report_evidence where report_id = #{reportId} "
            + "order by relevance_score desc, source_type asc, source_id asc")
    List<BizOperationReportEvidence> selectByReportId(Long reportId);
}
