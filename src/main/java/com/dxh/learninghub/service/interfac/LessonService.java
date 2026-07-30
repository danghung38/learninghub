package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.LessonRequest;
import com.dxh.learninghub.dto.request.LessonUpdateRequest;
import com.dxh.learninghub.dto.response.LessonResponse;

import java.util.List;

public interface LessonService {

    LessonResponse createLesson(LessonRequest request);

    LessonResponse updateLesson(Long id, LessonUpdateRequest request);

    void deleteLesson(Long id);

    LessonResponse getLessonById(Long id);

    List<LessonResponse> getLessonsByChapter(Long chapterId);
}
