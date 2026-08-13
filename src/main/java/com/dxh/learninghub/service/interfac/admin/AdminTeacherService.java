package com.dxh.learninghub.service.interfac.admin;

import com.dxh.learninghub.dto.response.admin.TeacherApplicationDetailResponse;

public interface AdminTeacherService {
    void approveTeacherRegistration(Long userId);
    void rejectTeacherRegistration(Long userId, String reason);
    void removeTeacherRole(Long userId);

    TeacherApplicationDetailResponse getUserApplicationDetail(Long userId);
}