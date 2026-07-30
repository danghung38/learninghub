package com.dxh.learninghub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record TeacherUpdateRequest(
    String expertise,

    @Min(value = 1, message = "MIN_INVALID")
    Double yearsOfExperience,

    String bio,

    @Pattern(
            regexp = "^(https?://)?(www\\.)?facebook\\.com/.+$",
            message = "INVALID_FB_URL"
    )
    String facebookLink
) {}
