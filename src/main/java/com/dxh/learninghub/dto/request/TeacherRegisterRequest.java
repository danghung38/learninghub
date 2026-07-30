package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record TeacherRegisterRequest(
    @NotBlank(message = "INVALID_BLANK")
    String expertise,

    @NotNull(message = "INVALID_NULL")
    @Min(value = 1, message = "MIN_INVALID")
    Double yearsOfExperience,

    @NotBlank(message = "INVALID_BLANK")
    String bio,

    @NotBlank(message = "INVALID_BLANK")
    @Pattern(
            regexp = "^(https?://)?(www\\.)?facebook\\.com/.+$",
            message = "INVALID_FB_URL"
    )
    String facebookLink
) {}
