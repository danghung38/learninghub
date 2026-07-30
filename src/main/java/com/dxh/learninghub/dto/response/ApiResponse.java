package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ApiResponse<T>(
    int code,

    String message,

    T result
) implements Serializable {
    public ApiResponse {
        if (code == 0) {
            code = 200;
        }
    }
}
