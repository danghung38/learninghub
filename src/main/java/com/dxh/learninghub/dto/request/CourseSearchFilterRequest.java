package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.validator.EnumValue;
import lombok.Builder;

@Builder
public record CourseSearchFilterRequest(
        // Các trường lọc của Course
        String title,
        String language,

        @EnumValue(enumClass = CourseLevel.class, message = "INVALID_COURSE_LEVEL")
        String courseLevel,

        Long minPoints,
        Long maxPoints,

        @EnumValue(enumClass = CourseStatus.class, message = "INVALID_COURSE_STATUS")
        String status,

        // Các trường lọc của Author
        String authorName,
        String authorExpertise
) {}