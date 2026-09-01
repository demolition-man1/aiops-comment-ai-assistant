package com.aiops.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AiExecutionDetailSchemaContractTest {

    @Test
    void executionDetailSchemaKeepsDurableIdempotencyAndLeaseFields() throws IOException {
        String schema = Files.readString(Path.of("src/main/resources/sql/schema.sql"));
        String upgrade = Files.readString(Path.of("src/main/resources/sql/upgrade-2026-09-01-ai-execution-detail.sql"));

        assertExecutionDetailContract(schema);
        assertExecutionDetailContract(upgrade);
        assertThat(executionDetailDefinition(schema).toLowerCase(Locale.ROOT))
                .doesNotContain("api_key", "access_key", "password");
    }

    private void assertExecutionDetailContract(String sql) {
        assertThat(sql).contains("create table if not exists biz_ai_execution_detail");
        assertThat(sql).contains("request_hash char(64) not null");
        assertThat(sql).contains("unique key uk_ai_execution_idempotency (idempotency_hash)");
        assertThat(sql).contains("index idx_ai_execution_lease (lease_until)");
    }

    private String executionDetailDefinition(String schema) {
        int start = schema.indexOf("create table if not exists biz_ai_execution_detail");
        int end = schema.indexOf(");", start);
        return schema.substring(start, end + 2);
    }
}
