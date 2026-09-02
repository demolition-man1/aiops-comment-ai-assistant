package com.aiops.service.aijob;

import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.properties.AiJobProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiJobLeaseService {

    private final BizAiExecutionDetailMapper detailMapper;
    private final AiJobProperties properties;

    public boolean claim(BizAiExecutionDetail detail, String workerId) {
        if (detail == null || detail.getTaskId() == null || detail.getVersion() == null || blank(workerId)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return detailMapper.claimLease(detail.getTaskId(), detail.getVersion(), workerId,
                now.plusSeconds(properties.getLeaseSeconds()), now) == 1;
    }

    public boolean renew(Long taskId, String workerId) {
        if (taskId == null || blank(workerId)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return detailMapper.renewLease(taskId, workerId, now.plusSeconds(properties.getLeaseSeconds()), now) == 1;
    }

    public void release(Long taskId, String workerId) {
        if (taskId != null && !blank(workerId)) {
            detailMapper.releaseLease(taskId, workerId, LocalDateTime.now());
        }
    }

    public boolean isLive(BizAiExecutionDetail detail) {
        return detail != null && detail.getLeaseUntil() != null && detail.getLeaseUntil().isAfter(LocalDateTime.now());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
