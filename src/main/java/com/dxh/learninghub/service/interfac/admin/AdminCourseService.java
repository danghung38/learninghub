package com.dxh.learninghub.service.interfac.admin;

import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.enums.CourseStatus;
import org.springframework.data.domain.Pageable;


public interface AdminCourseService {
    CourseResponse approve(Long id);

    PageResponse<CourseResponse> getByStatus(CourseStatus status, Pageable pageable);

    PageResponse<CourseResponse> searchCourses(Pageable pageable, String[] course, String[] author, CourseStatus status);

    void ban(Long id);

    void unban(Long id);

    CourseResponse reject(Long id);
}
