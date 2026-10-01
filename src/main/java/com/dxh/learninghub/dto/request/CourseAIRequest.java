package com.dxh.learninghub.dto.request;

import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.CourseStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record CourseAIRequest(
        Long id,

        Integer version,

        TeacherAIRequest teacher,

        String title,

        String description,

        String language,

        Long points,

        CourseLevel courseLevel,

        CourseStatus status,

        List<ChapterAIRequest> chapters
) {
}
