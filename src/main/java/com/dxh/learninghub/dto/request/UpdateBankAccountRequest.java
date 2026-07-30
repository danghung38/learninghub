package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateBankAccountRequest(
        @Size(min = 1, max = 100, message = "CONTENT_TOO_LONG")
        String bankName,

        @Size(min = 1, max = 50, message = "CONTENT_TOO_LONG")
        String accountNumber,

        @Size(min = 1, max = 150, message = "CONTENT_TOO_LONG")
        String accountHolder,

        Boolean isDefault
) {
}
