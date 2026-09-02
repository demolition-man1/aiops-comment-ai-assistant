package com.aiops.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiJobObservabilitySchemaContractTest {

    @Test
    void callLogSchemaIncludesJobAndTimingFieldsWithoutSensitiveColumns() throws IOException {
        String schema = Files.readString(resource("sql/schema.sql"));
        String upgrade = Files.readString(resource("sql/upgrade-2026-09-01-ai-job-observability.sql"));
        String combined = (schema + upgrade).toLowerCase();

        assertThat(combined).contains("job_id", "queue_latency_ms", "total_latency_ms", "error_code");
        assertThat(combined).doesNotContain("api_key", "access_key", "authorization");
    }

    private Path resource(String name) {
        return Path.of("src/main/resources").resolve(name);
    }
}
