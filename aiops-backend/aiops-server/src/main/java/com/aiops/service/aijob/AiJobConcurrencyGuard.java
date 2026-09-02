package com.aiops.service.aijob;

import com.aiops.properties.AiJobProperties;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AiJobConcurrencyGuard {

    private static final String GLOBAL_KEY = "aiops:ai-job:permit:global";
    private static final String USER_PREFIX = "aiops:ai-job:permit:user:";

    private final PermitBackend backend;
    private final AiJobProperties properties;

    public AiJobConcurrencyGuard(RedissonClient redissonClient, AiJobProperties properties) {
        this(new RedissonPermitBackend(redissonClient), properties);
    }

    AiJobConcurrencyGuard(PermitBackend backend, AiJobProperties properties) {
        this.backend = backend;
        this.properties = properties;
    }

    public Optional<Permit> tryAcquire(Long userId) {
        if (userId == null || userId <= 0) {
            return Optional.empty();
        }
        properties.validate();
        Duration lease = Duration.ofSeconds(properties.getLeaseSeconds());
        String globalPermit = backend.tryAcquire(GLOBAL_KEY, properties.getGlobalConcurrency(), lease);
        if (globalPermit == null) {
            return Optional.empty();
        }
        String userKey = USER_PREFIX + userId;
        String userPermit = backend.tryAcquire(userKey, properties.getPerUserConcurrency(), lease);
        if (userPermit == null) {
            backend.release(GLOBAL_KEY, globalPermit);
            return Optional.empty();
        }
        return Optional.of(new Permit(GLOBAL_KEY, globalPermit, userKey, userPermit));
    }

    public void release(Permit permit) {
        if (permit == null) {
            return;
        }
        backend.release(permit.userKey(), permit.userPermitId());
        backend.release(permit.globalKey(), permit.globalPermitId());
    }

    public record Permit(String globalKey, String globalPermitId, String userKey, String userPermitId) {
    }

    interface PermitBackend {
        String tryAcquire(String key, int limit, Duration leaseDuration);

        void release(String key, String permitId);
    }

    @RequiredArgsConstructor
    static final class RedissonPermitBackend implements PermitBackend {
        private final RedissonClient redissonClient;

        @Override
        public String tryAcquire(String key, int limit, Duration leaseDuration) {
            RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
            semaphore.trySetPermits(limit);
            try {
                return semaphore.tryAcquire(0, leaseDuration.toSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        @Override
        public void release(String key, String permitId) {
            if (permitId != null) {
                redissonClient.getPermitExpirableSemaphore(key).tryRelease(permitId);
            }
        }
    }
}
