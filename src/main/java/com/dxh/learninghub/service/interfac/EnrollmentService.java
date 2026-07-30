package com.dxh.learninghub.service.interfac;


import com.dxh.learninghub.dto.request.BuyCourseRequest;
import com.dxh.learninghub.dto.response.BuyCourseResponse;
import com.dxh.learninghub.dto.response.EnrollmentStatusResponse;
import com.dxh.learninghub.dto.response.MyCourseResponse;

import java.util.List;

public interface EnrollmentService {
    List<MyCourseResponse> getCourseByUserCurrent();
    BuyCourseResponse buyCourse(BuyCourseRequest request);

    EnrollmentStatusResponse getEnrollmentStatus(Long courseId);
}
