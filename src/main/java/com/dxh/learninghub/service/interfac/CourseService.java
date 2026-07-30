package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.CourseSearchFilterRequest;
import com.dxh.learninghub.dto.request.CourseUpdateRequest;
import com.dxh.learninghub.dto.request.CourseUploadRequest;
import com.dxh.learninghub.dto.response.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseService {

    CourseResponse getCourse(Long courseId);

    List<String> getTitleSuggestions(String query);

    CoursePreviewResponse getCoursePreview(Long courseId);

    CourseManagementPreviewResponse getManagementPreview(Long courseId);

    List<CourseResponse> getMyCourses();

    CourseUploadResponse createCourse(CourseUploadRequest request, MultipartFile thumbnail);

    CourseUploadResponse updateCourse(Long courseId, CourseUpdateRequest request, MultipartFile thumbnail);

    CourseUploadResponse submitCourse(Long courseId);

    void softDeleteCourse(Long courseId);

    PageResponse<CourseResponse> searchCourses(Pageable pageable, CourseSearchFilterRequest filter);
}
