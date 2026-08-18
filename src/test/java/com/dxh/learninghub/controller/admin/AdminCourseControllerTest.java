package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.dto.request.RejectRequest;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.interfac.admin.AdminCourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminCourseService adminCourseService;

    @Test
    void approve_returnsUpdatedCourse() throws Exception {
        when(adminCourseService.approve(5L))
                .thenReturn(CourseResponse.builder().id(5L).title("Java Backend").build());

        mockMvc.perform(patch("/admin/course/{id}/approve", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Approve successfully"))
                .andExpect(jsonPath("$.result.id").value(5));
    }

    @Test
    void reject_passesAdministrativeReasonToService() throws Exception {
        when(adminCourseService.reject(5L, "Nội dung chưa đầy đủ"))
                .thenReturn(CourseResponse.builder().id(5L).build());

        mockMvc.perform(patch("/admin/course/{id}/reject", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RejectRequest.builder().reason("Nội dung chưa đầy đủ").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reject successfully"));

        verify(adminCourseService).reject(5L, "Nội dung chưa đầy đủ");
    }

    @Test
    void ban_passesAdministrativeReasonToService() throws Exception {
        doNothing().when(adminCourseService).ban(5L, "Vi phạm nội dung");

        mockMvc.perform(patch("/admin/course/{id}/ban", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RejectRequest.builder().reason("Vi phạm nội dung").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ban successfully"));

        verify(adminCourseService).ban(5L, "Vi phạm nội dung");
    }

    @Test
    void approve_whenCourseIsNotPending_returnsMappedError() throws Exception {
        when(adminCourseService.approve(5L))
                .thenThrow(new AppException(ErrorCode.COURSE_NOT_PENDING));

        mockMvc.perform(patch("/admin/course/{id}/approve", 5L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.COURSE_NOT_PENDING.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.COURSE_NOT_PENDING.getMessage()));
    }
}
