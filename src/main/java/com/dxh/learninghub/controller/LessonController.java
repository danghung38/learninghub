package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.LessonRequest;
import com.dxh.learninghub.dto.request.LessonUpdateRequest;
import com.dxh.learninghub.dto.request.PresignedUploadRequest;
import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.LessonResponse;
import com.dxh.learninghub.dto.response.PresignedUploadResponse;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Lessons", description = "APIs for accessing and managing course lessons")
public class LessonController {

    LessonService lessonService;
    AwsS3Service awsS3Service;

    @Operation(summary = "Create a document upload URL", description = "Generate a presigned URL for uploading lesson documents")
    @PostMapping("/documents/presigned-url")
    public ApiResponse<PresignedUploadResponse> generateDocumentUrl(
            @RequestBody @Valid PresignedUploadRequest request) {

        return ApiResponse.<PresignedUploadResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Create presigned-url Successfully")
                .result(awsS3Service.generateDocumentUploadUrl(
                        request.fileName(), request.fileSize()))
                .build();
    }

    @Operation(summary = "Create a video upload URL", description = "Generate a presigned URL for uploading lesson videos")
    @PostMapping("/videos/presigned-url")
    public ApiResponse<PresignedUploadResponse> generateVideoUrl(
            @RequestBody @Valid PresignedUploadRequest request) {

        return ApiResponse.<PresignedUploadResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Create presigned-url Successfully")
                .result(awsS3Service.generateVideoUploadUrl(
                        request.fileName(), request.fileSize()))
                .build();
    }

    @Operation(summary = "Create a lesson", description = "Create a lesson under a chapter owned by the teacher")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonResponse> createLesson(@RequestBody @Valid LessonRequest request) {
        return ApiResponse.<LessonResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create Lesson Successfully")
                .result(lessonService.createLesson(request))
                .build();
    }

    @Operation(summary = "Update a lesson", description = "Update an existing lesson and its content metadata")
    @PatchMapping("/{id}")
    public ApiResponse<LessonResponse> updateLesson(
            @PathVariable Long id,
            @RequestBody @Valid LessonUpdateRequest request) {

        return ApiResponse.<LessonResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Update Lesson Successfully")
                .result(lessonService.updateLesson(id, request))
                .build();
    }

    @Operation(summary = "Delete a lesson", description = "Delete a lesson as its course owner or an administrator")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLesson(@PathVariable Long id) {
        lessonService.deleteLesson(id);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Delete Lesson Successfully")
                .build();
    }

    @Operation(summary = "Get a lesson", description = "Return lesson details when the current user has access")
    @GetMapping("/{id}")
    public ApiResponse<LessonResponse> getLessonById(@PathVariable Long id) {
        return ApiResponse.<LessonResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get Lesson Successfully")
                .result(lessonService.getLessonById(id))
                .build();
    }

    @Operation(summary = "Get lessons by chapter", description = "Return accessible lessons belonging to a chapter")
    @GetMapping("/chapter/{chapterId}")
    public ApiResponse<List<LessonResponse>> getLessonsByChapter(@PathVariable Long chapterId) {
        return ApiResponse.<List<LessonResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get Lessons Successfully")
                .result(lessonService.getLessonsByChapter(chapterId))
                .build();
    }
}
