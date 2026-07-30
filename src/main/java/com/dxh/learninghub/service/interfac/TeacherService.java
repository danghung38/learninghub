package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.TeacherRegisterRequest;
import com.dxh.learninghub.dto.request.TeacherUpdateRequest;
import com.dxh.learninghub.dto.response.TeacherDashboardResponse;
import com.dxh.learninghub.dto.response.TeacherResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.TeacherCourseStudentResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface TeacherService {
    TeacherResponse registerTeacher(TeacherRegisterRequest request, MultipartFile cv, MultipartFile certificate);
    TeacherResponse reRegisterTeacher(TeacherRegisterRequest request, MultipartFile cv, MultipartFile certificate);
    TeacherResponse updateTeacherProfile(TeacherUpdateRequest request, MultipartFile cv, MultipartFile certificate);

    TeacherResponse getTeacherProfile();

    PageResponse<TeacherCourseStudentResponse> getCourseStudents(
            Long courseId,
            Pageable pageable);
}
