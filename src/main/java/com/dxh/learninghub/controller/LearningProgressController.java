package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.CourseProgressResponse;
import com.dxh.learninghub.service.interfac.LearningProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lesson-progress")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Learning Progress", description = "APIs for completing lessons and tracking course progress")
public class LearningProgressController {

    LearningProgressService learningProgressService;

    @Operation(summary = "Complete a lesson", description = "Mark a lesson as completed for the current user")
    @PostMapping("/{lessonId}/complete")
    public ApiResponse<Void> completeLesson(
            @PathVariable Long lessonId) {

        learningProgressService.completeLesson(lessonId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Lesson marked as completed")
                .build();
    }

    @Operation(summary = "Get course progress", description = "Return the current user's learning progress for a course")
    @GetMapping("/courses/{courseId}")
    public ApiResponse<CourseProgressResponse> getProgress(
            @PathVariable Long courseId) {

        return ApiResponse.<CourseProgressResponse>builder()
                .code(HttpStatus.OK.value())
                .result(learningProgressService.getCourseProgress(courseId))
                .build();
    }
}
