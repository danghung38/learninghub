package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.CourseSearchFilterRequest;
import com.dxh.learninghub.dto.request.CourseUploadRequest;
import com.dxh.learninghub.dto.request.CourseUpdateRequest;
import com.dxh.learninghub.dto.request.PresignedUploadRequest;
import com.dxh.learninghub.dto.response.*;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.CourseService;
import com.dxh.learninghub.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Courses", description = "APIs for browsing and managing courses")
public class CourseController {
    CourseService courseService;
    AwsS3Service awsS3Service;

    @Operation(summary = "Get a course", description = "Return details for an approved course")
    @GetMapping("/{courseId}")
    public ApiResponse<CourseResponse> getCourse(@PathVariable Long courseId) {
        return ApiResponse.<CourseResponse>builder()
                .message("Get course successfully")
                .result(courseService.getCourse(courseId))
                .build();
    }

    @Operation(summary = "Search courses", description = "Search, filter, and paginate approved courses")
    @GetMapping("/list")
    public ApiResponse<PageResponse<CourseResponse>> searchCourses(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @ParameterObject @Valid CourseSearchFilterRequest filter) {

        Pageable pageable = PageUtil.createPageable(
                pageNo, pageSize, sortBy,
                "id", "title", "points", "duration",
                "totalEnrollments", "createdAt", "updatedAt");

        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Successfully get course list")
                .result(courseService.searchCourses(pageable, filter))
                .build();
    }


    @Operation(summary = "Get title suggestions", description = "Return approved course title suggestions for a query")
    @GetMapping("/title")
    public ApiResponse<List<String>> getTitleSuggestions(@RequestParam("query") String query) {
        List<String> suggestions = courseService.getTitleSuggestions(query);
        return ApiResponse.<List<String>>builder()
                .code(HttpStatus.OK.value())
                .message("Suggestions fetched successfully")
                .result(suggestions).build();
    }


    @Operation(
            summary = "Preview a course curriculum",
            description = "Return course, chapter, and lesson metadata without lesson content URLs"
    )
    @GetMapping("/{courseId}/preview")
    public ApiResponse<CoursePreviewResponse> getCoursePreview(@PathVariable Long courseId) {
        return ApiResponse.<CoursePreviewResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get course preview successfully")
                .result(courseService.getCoursePreview(courseId))
                .build();
    }

    @Operation(
            summary = "Preview complete course content",
            description = "Allow an administrator or the course owner to preview chapters, lessons, and lesson content URLs"
    )
    @GetMapping("/{courseId}/management-preview")
    public ApiResponse<CourseManagementPreviewResponse> getManagementPreview(
            @PathVariable Long courseId) {
        return ApiResponse.<CourseManagementPreviewResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get complete course preview successfully")
                .result(courseService.getManagementPreview(courseId))
                .build();
    }

    @Operation(summary = "Get my authored courses", description = "Return courses authored by the current teacher")
    @GetMapping("/my-courses")
    public ApiResponse<List<CourseResponse>> myCourses() {
        return ApiResponse.<List<CourseResponse>>builder()
                .code(HttpStatus.OK.value())
                .result(courseService.getMyCourses())
                .build();
    }


    @Operation(summary = "Create a video upload URL", description = "Generate a presigned URL for uploading a course video")
    @PostMapping("/videos/presigned-url")
    public ApiResponse<PresignedUploadResponse> generateVideoUrl(
            @RequestBody @Valid PresignedUploadRequest request) {

        return ApiResponse.<PresignedUploadResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Create presigned-url Successfully")
                .result(awsS3Service.generateVideoUploadUrl(request.fileName(),request.fileSize()))
                .build();
    }

    @Operation(
            summary = "Create a course",
            description = "Create a pending course without notifying an administrator until the teacher submits it")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseUploadResponse> uploadCourse(
            @RequestPart("course") @Valid CourseUploadRequest request,
            @RequestPart("thumbnail") MultipartFile thumbnail) {

        return ApiResponse.<CourseUploadResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Create Course Successfully")
                .result(courseService.createCourse(request, thumbnail)).build();
    }

    @Operation(
            method = "PATCH",
            summary = "Update a course",
            description = "Update course information without notifying an administrator"
    )
    @PatchMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CourseUploadResponse> updateCourse(
            @PathVariable Long courseId,
            @RequestPart("course") @Valid CourseUpdateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {

        return ApiResponse.<CourseUploadResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Update course successfully")
                .result(courseService.updateCourse(courseId, request, thumbnail))
                .build();
    }

    @Operation(
            summary = "Submit a course for approval",
            description = "Move a draft course to pending review and notify an administrator")
    @PatchMapping("/{courseId}/submit")
    public ApiResponse<CourseUploadResponse> submitCourse(
            @PathVariable Long courseId) {
        return ApiResponse.<CourseUploadResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Submit course for approval successfully")
                .result(courseService.submitCourse(courseId))
                .build();
    }

    @Operation(
            summary = "Soft-delete a course",
            description = "Mark a course as deleted without physically removing its data")
    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> softDeleteCourse(@PathVariable Long courseId) {
        courseService.softDeleteCourse(courseId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Soft-delete course successfully")
                .build();
    }

}
