package com.aiops.service.impl;

import com.aiops.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements CacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void set(String key, Object value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ignored) {
            // Redis is an acceleration layer; MySQL remains the source of truth.
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, type));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ignored) {
            // Redis cache delete failure should not block business flow.
        }
    }

    @Override
    public boolean allow(String key, long limit, Duration window) {
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, window);
            }
            return count == null || count <= limit;
        } catch (Exception ignored) {
            return true;
        }
    }
}
