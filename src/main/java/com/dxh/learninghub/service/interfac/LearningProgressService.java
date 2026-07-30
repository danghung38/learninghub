package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.response.CourseProgressResponse;

public interface LearningProgressService {

    void completeLesson(Long lessonId);

    CourseProgressResponse getCourseProgress(Long courseId);

    void synchronizeCourse(Long courseId);
}
