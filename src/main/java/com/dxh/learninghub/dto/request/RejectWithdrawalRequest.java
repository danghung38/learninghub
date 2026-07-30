package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectWithdrawalRequest(
        @NotBlank(message = "INVALID_BLANK")
        @Size(max = 500, message = "CONTENT_TOO_LONG")
        String reason
) {
}
