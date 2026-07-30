package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record CreateBankAccountRequest(
        @NotBlank(message = "INVALID_BLANK")
        @Size(max = 100, message = "CONTENT_TOO_LONG")
        String bankName,

        @NotBlank(message = "INVALID_BLANK")
        @Size(max = 50, message = "CONTENT_TOO_LONG")
        String accountNumber,

        @NotBlank(message = "INVALID_BLANK")
        @Size(max = 150, message = "CONTENT_TOO_LONG")
        String accountHolder,

        @NotNull(message = "INVALID_NULL")
        Boolean isDefault
) {
}
