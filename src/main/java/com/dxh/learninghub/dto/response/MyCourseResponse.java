package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.EnrollmentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyCourseResponse(
    Long courseId,

    String title,

    String author,

    CourseLevel courseLevel,

    String thumbnail,

    Long points,

    EnrollmentStatus enrollmentStatus,

    LocalDateTime enrolledAt
) {}
