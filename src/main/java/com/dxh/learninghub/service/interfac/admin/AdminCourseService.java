package com.dxh.learninghub.service.interfac.admin;

import com.dxh.learninghub.dto.request.CourseSearchFilterRequest;
import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;


public interface AdminCourseService {
    CourseResponse approve(Long id);

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    PageResponse<CourseResponse> searchCourses(Pageable pageable, CourseSearchFilterRequest filter);

    void ban(Long id, String reason);

    void unban(Long id);

    CourseResponse reject(Long id, String reason);
}
