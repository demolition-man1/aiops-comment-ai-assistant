package com.aiops.service.aijob;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiJobHandlerRegistryTest {

    @Test
    void resolvesTheHandlerForEachPhaseTwoJobType() {
        AiJobHandler report = new StubHandler("operation_report");
        AiJobHandler compare = new StubHandler("product_compare");
        AiJobHandlerRegistry registry = new AiJobHandlerRegistry(List.of(report, compare));

        assertThat(registry.require("operation_report")).isSameAs(report);
        assertThat(registry.require("product_compare")).isSameAs(compare);
        assertThatThrownBy(() -> registry.require("content_generation"))
                .hasMessageContaining("不支持");
    }

    private record StubHandler(String jobType) implements AiJobHandler {
        @Override
        public AiJobExecutionResult execute(com.aiops.entity.BizAnalysisTask task) {
            throw new UnsupportedOperationException();
        }
    }
}
