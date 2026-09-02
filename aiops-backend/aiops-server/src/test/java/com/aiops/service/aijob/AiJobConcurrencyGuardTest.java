package com.aiops.service.aijob;

import com.aiops.properties.AiJobProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AiJobConcurrencyGuardTest {

    @Test
    void releasesGlobalPermitWhenUserLimitRejectsTheJob() {
        FakePermitBackend backend = new FakePermitBackend("global-permit", null);
        AiJobConcurrencyGuard guard = new AiJobConcurrencyGuard(backend, properties());

        Optional<AiJobConcurrencyGuard.Permit> permit = guard.tryAcquire(8L);

        assertThat(permit).isEmpty();
        assertThat(backend.releasedGlobalPermit).isEqualTo("global-permit");
    }

    @Test
    void releaseReturnsBothPermitsAfterExecution() {
        FakePermitBackend backend = new FakePermitBackend("global-permit", "user-permit");
        AiJobConcurrencyGuard guard = new AiJobConcurrencyGuard(backend, properties());

        AiJobConcurrencyGuard.Permit permit = guard.tryAcquire(8L).orElseThrow();
        guard.release(permit);

        assertThat(backend.releasedGlobalPermit).isEqualTo("global-permit");
        assertThat(backend.releasedUserPermit).isEqualTo("user-permit");
    }

    private AiJobProperties properties() {
        AiJobProperties properties = new AiJobProperties();
        properties.setGlobalConcurrency(4);
        properties.setPerUserConcurrency(2);
        properties.setLeaseSeconds(45);
        return properties;
    }

    private static final class FakePermitBackend implements AiJobConcurrencyGuard.PermitBackend {
        private final String globalPermit;
        private final String userPermit;
        private String releasedGlobalPermit;
        private String releasedUserPermit;

        private FakePermitBackend(String globalPermit, String userPermit) {
            this.globalPermit = globalPermit;
            this.userPermit = userPermit;
        }

        @Override
        public String tryAcquire(String key, int limit, Duration leaseDuration) {
            return key.endsWith(":global") ? globalPermit : userPermit;
        }

        @Override
        public void release(String key, String permitId) {
            if (key.endsWith(":global")) {
                releasedGlobalPermit = permitId;
            } else {
                releasedUserPermit = permitId;
            }
        }
    }
}
