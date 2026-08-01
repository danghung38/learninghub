package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdvertisementTestSendRequest(
        @NotNull(message = "INVALID_NULL")
        Long advertisementId,

        @NotBlank(message = "INVALID_BLANK")
        @Email(message = "INVALID_EMAIL")
        String email) {
}
