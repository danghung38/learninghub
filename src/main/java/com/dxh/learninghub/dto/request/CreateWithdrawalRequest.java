package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateWithdrawalRequest(
        @NotNull(message = "INVALID_NULL")
        @Min(value = 1, message = "MIN_INVALID")
        Long bankAccountId,

        @NotNull(message = "INVALID_NULL")
        @Min(value = 1, message = "MIN_INVALID")
        Long points
) {
}
