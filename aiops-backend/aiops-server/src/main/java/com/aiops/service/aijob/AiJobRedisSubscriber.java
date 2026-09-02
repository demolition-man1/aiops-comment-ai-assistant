package com.aiops.service.aijob;

import com.aiops.constant.RedisKeyConstant;
import com.aiops.service.AiJobEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AiJobRedisSubscriber {

    private static final Set<String> JOB_TYPES = Set.of(
            "operation_report", "product_compare", "negative_reply", "content");

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final AiJobEventService eventService;

    private int listenerId;

    @PostConstruct
    void subscribe() {
        RTopic topic = redissonClient.getTopic(RedisKeyConstant.AI_JOB_EVENT_CHANNEL);
        listenerId = topic.addListener(String.class, (channel, message) -> handle(message));
    }

    @PreDestroy
    void unsubscribe() {
        if (listenerId != 0) {
            redissonClient.getTopic(RedisKeyConstant.AI_JOB_EVENT_CHANNEL).removeListener(listenerId);
        }
    }

    void handle(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            long jobId = event.path("jobId").asLong(0);
            String eventType = event.path("eventType").asText();
            String jobType = event.path("jobType").asText();
            String stage = event.path("stage").asText();
            int progress = event.path("progress").asInt(-1);
            if (jobId > 0 && "stage".equals(eventType) && JOB_TYPES.contains(jobType)) {
                eventService.publishStage(jobId, stage, progress);
            } else if (jobId > 0 && "text_delta".equals(eventType)
                    && Set.of("negative_reply", "content").contains(jobType)) {
                eventService.publishTextDelta(jobId, event.path("text").asText(), event.path("deltaId").asLong(0));
            }
        } catch (Exception ignored) {
            // Redis progress delivery is best effort; MySQL remains authoritative.
        }
    }
}
