package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateVNPayDepositRequest(
        @NotNull(message = "INVALID_NULL")
        @Min(value = 1_000, message = "MIN_INVALID")
        @Max(value = 50_000_000L, message = "MAX_INVALID")
        Long amount
) {
}
