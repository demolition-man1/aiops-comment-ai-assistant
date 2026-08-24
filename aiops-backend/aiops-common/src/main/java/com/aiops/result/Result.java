package com.aiops.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String msg;
    private T data;
    private LocalDateTime timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, LocalDateTime.now());
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null, LocalDateTime.now());
    }
}

