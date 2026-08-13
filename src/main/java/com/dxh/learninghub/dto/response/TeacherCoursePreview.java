package com.dxh.learninghub.dto.response;

import lombok.Builder;

/**
 * Public teacher information embedded in a course preview.
 * Sensitive account fields are intentionally excluded.
 */
@Builder
public record TeacherCoursePreview(
        Long id,
        String fullName,
        String avatar,
        String expertise,
        Double yearsOfExperience,
        String bio,
        String facebookLink
) {}
