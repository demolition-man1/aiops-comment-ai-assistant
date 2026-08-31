package com.aiops.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RagNegativeReplySchemaContractTest {

    @Test
    void negativeReplySchemaKeepsRagProvenanceColumnsAndIdempotentUpgrade() throws IOException {
        String schema = Files.readString(Path.of("src/main/resources/sql/schema.sql"));
        String upgrade = Files.readString(Path.of("src/main/resources/sql/upgrade-2026-08-30-rag-negative-reply.sql"));

        assertThat(schema).contains("rag_used tinyint not null default 0");
        assertThat(schema).contains("rag_references json null");
        assertThat(upgrade).contains("information_schema.columns");
        assertThat(upgrade).contains("column_name = 'rag_used'");
        assertThat(upgrade).contains("column_name = 'rag_references'");
    }

    @Test
    void reportEvidenceSchemaKeepsReferencesInAnAdditiveRelationTable() throws IOException {
        String schema = Files.readString(Path.of("src/main/resources/sql/schema.sql"));
        String upgrade = Files.readString(Path.of("src/main/resources/sql/upgrade-2026-08-31-report-evidence.sql"));

        assertThat(schema).contains("create table if not exists biz_operation_report_evidence");
        assertThat(schema).contains("unique key uk_operation_report_evidence_source");
        assertThat(upgrade).contains("create table if not exists biz_operation_report_evidence");
        assertThat(upgrade).contains("retrieval_version varchar(64) not null");
    }
}
