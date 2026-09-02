package com.aiops.service.aijob;

import com.aiops.entity.BizAiExecutionDetail;
import com.aiops.mapper.BizAiExecutionDetailMapper;
import com.aiops.properties.AiJobProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobLeaseServiceTest {

    @Mock
    private BizAiExecutionDetailMapper detailMapper;

    @Test
    void renewsOnlyTheLeaseOwnedByThisWorker() {
        AiJobLeaseService service = new AiJobLeaseService(detailMapper, properties());
        when(detailMapper.renewLease(eq(19L), eq("worker-a"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1, 0);

        assertThat(service.renew(19L, "worker-a")).isTrue();
        assertThat(service.renew(19L, "worker-b")).isFalse();
    }

    @Test
    void detectsLiveLeaseFromDetailTimestamp() {
        AiJobLeaseService service = new AiJobLeaseService(detailMapper, properties());
        BizAiExecutionDetail detail = new BizAiExecutionDetail();
        detail.setLeaseUntil(LocalDateTime.now().plusSeconds(5));

        assertThat(service.isLive(detail)).isTrue();
        detail.setLeaseUntil(LocalDateTime.now().minusSeconds(1));
        assertThat(service.isLive(detail)).isFalse();
    }

    private AiJobProperties properties() {
        AiJobProperties properties = new AiJobProperties();
        properties.setLeaseSeconds(45);
        return properties;
    }
}
