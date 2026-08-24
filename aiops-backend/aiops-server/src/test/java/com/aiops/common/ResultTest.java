package com.aiops.common;

import com.aiops.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void successWrapsDataWithCode200() {
        Result<String> result = Result.success("ok");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("ok");
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    void errorWrapsCodeAndMessage() {
        Result<Void> result = Result.error(400, "参数错误");

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMsg()).isEqualTo("参数错误");
        assertThat(result.getData()).isNull();
        assertThat(result.getTimestamp()).isNotNull();
    }
}
