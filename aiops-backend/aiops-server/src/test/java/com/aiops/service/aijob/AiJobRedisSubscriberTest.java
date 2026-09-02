package com.aiops.service.aijob;

import com.aiops.service.AiJobEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiJobRedisSubscriberTest {

    @Test
    void forwardsTextDeltasOnlyForSupportedTextJobs() {
        AiJobEventService eventService = mock(AiJobEventService.class);
        AiJobRedisSubscriber subscriber = new AiJobRedisSubscriber(mock(RedissonClient.class), new ObjectMapper(), eventService);

        subscriber.handle("{\"eventType\":\"text_delta\",\"jobId\":9,\"jobType\":\"negative_reply\",\"text\":\"hello\",\"deltaId\":2}");
        subscriber.handle("{\"eventType\":\"text_delta\",\"jobId\":10,\"jobType\":\"operation_report\",\"text\":\"{\",\"deltaId\":1}");

        verify(eventService).publishTextDelta(9L, "hello", 2L);
    }
}
