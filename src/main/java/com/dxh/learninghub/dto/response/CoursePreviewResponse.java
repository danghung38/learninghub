package com.dxh.learninghub.dto.response;

import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.CourseStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record CoursePreviewResponse(
    Long id,

    TeacherCoursePreview teacher,

    String title,

    String description,

    Long points,

    Integer duration,

    String language,

    CourseLevel courseLevel,

    String thumbnail,

    String videoUrl,

    Long totalEnrollments,

    CourseStatus status,

    List<ChapterPreviewResponse> chapters
) {}
