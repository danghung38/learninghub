package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.BuyCourseRequest;
import com.dxh.learninghub.dto.response.BuyCourseResponse;
import com.dxh.learninghub.dto.response.EnrollmentStatusResponse;
import com.dxh.learninghub.enums.EnrollmentStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.interfac.EnrollmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnrollmentService enrollmentService;

    @Test
    void buyCourse_withValidCourseId_returnsPurchasedCourse() throws Exception {
        when(enrollmentService.buyCourse(any(BuyCourseRequest.class)))
                .thenReturn(BuyCourseResponse.builder()
                        .courseId(7L)
                        .title("Spring Boot thực chiến")
                        .points(100L)
                        .build());

        mockMvc.perform(post("/enrollments/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BuyCourseRequest.builder().courseId(7L).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.courseId").value(7))
                .andExpect(jsonPath("$.result.title").value("Spring Boot thực chiến"));
    }

    @Test
    void buyCourse_withInvalidCourseId_returnsValidationError() throws Exception {
        mockMvc.perform(post("/enrollments/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BuyCourseRequest.builder().courseId(0L).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MIN_INVALID.getCode()));
    }

    @Test
    void buyCourse_whenPointsAreInsufficient_returnsMappedBusinessError() throws Exception {
        when(enrollmentService.buyCourse(any(BuyCourseRequest.class)))
                .thenThrow(new AppException(ErrorCode.BUY_COURSE_INVALID));

        mockMvc.perform(post("/enrollments/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BuyCourseRequest.builder().courseId(7L).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.BUY_COURSE_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.BUY_COURSE_INVALID.getMessage()));
    }

    @Test
    void getMyCourses_returnsCurrentUserCourses() throws Exception {
        when(enrollmentService.getCourseByUserCurrent()).thenReturn(List.of());

        mockMvc.perform(get("/enrollments/my-courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("My Courses"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    void getEnrollmentStatus_returnsProgressState() throws Exception {
        when(enrollmentService.getEnrollmentStatus(7L))
                .thenReturn(EnrollmentStatusResponse.builder()
                        .courseId(7L)
                        .enrolled(true)
                        .status(EnrollmentStatus.ACTIVE)
                        .build());

        mockMvc.perform(get("/enrollments/courses/{courseId}/status", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.courseId").value(7))
                .andExpect(jsonPath("$.result.enrolled").value(true));

        verify(enrollmentService).getEnrollmentStatus(7L);
    }
}
