package com.dxh.learninghub.dto.request;

import lombok.Builder;

@Builder
public record TeacherAIRequest(
        Long id,

        String fullName,

        String expertise,

        Integer yearsOfExperience,

        String bio
) {
}