package com.aiops.service.impl;

import com.aiops.enumeration.TaskStatusEnum;
import com.aiops.vo.AiJobCreatedVO;
import com.aiops.vo.AiJobVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiJobOwnershipTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publicJobViewsUseCamelCaseAndDoNotExposeSensitiveExecutionFields() throws Exception {
        AiJobCreatedVO created = new AiJobCreatedVO(42L, "pending", false);
        AiJobVO job = new AiJobVO();
        job.setJobId(42L);
        job.setTaskStatus("pending");
        job.setJobStage("preparing");
        job.setTargetType("product");
        job.setTargetId("product-1");

        assertThat(objectMapper.writeValueAsString(created))
                .contains("\"jobId\":42", "\"taskStatus\":\"pending\"", "\"reused\":false");
        assertThat(objectMapper.writeValueAsString(job))
                .contains("\"jobId\":42", "\"jobStage\":\"preparing\"")
                .doesNotContain("requestParam", "idempotencyHash", "leaseOwner", "promptTemplate");
    }

    @Test
    void taskStatusesIncludeTerminalAiOutcomes() {
        assertThat(TaskStatusEnum.valueOf("TIMED_OUT").name()).isEqualTo("TIMED_OUT");
        assertThat(TaskStatusEnum.valueOf("CANCELLED").name()).isEqualTo("CANCELLED");
    }
}
