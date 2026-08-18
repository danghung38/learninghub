package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.CourseSearchFilterRequest;
import com.dxh.learninghub.dto.request.PresignedUploadRequest;
import com.dxh.learninghub.dto.response.CourseResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.PresignedUploadResponse;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    @MockBean
    private AwsS3Service awsS3Service;

    @Test
    void getCourse_returnsApprovedCourse() throws Exception {
        when(courseService.getCourse(3L)).thenReturn(course(3L));

        mockMvc.perform(get("/courses/{courseId}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Get course successfully"))
                .andExpect(jsonPath("$.result.id").value(3))
                .andExpect(jsonPath("$.result.title").value("Java Backend"));
    }

    @Test
    void getCourse_whenCourseDoesNotExist_returnsMappedError() throws Exception {
        when(courseService.getCourse(404L))
                .thenThrow(new AppException(ErrorCode.COURSE_NOT_EXISTED));

        mockMvc.perform(get("/courses/{courseId}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.COURSE_NOT_EXISTED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.COURSE_NOT_EXISTED.getMessage()));
    }

    @Test
    void searchCourses_buildsPageableAndBindsFilter() throws Exception {
        PageResponse<CourseResponse> page = PageResponse.<CourseResponse>builder()
                .pageNo(2)
                .pageSize(5)
                .totalPage(1)
                .totalElements(1)
                .items(List.of(course(3L)))
                .build();
        when(courseService.searchCourses(any(Pageable.class), any(CourseSearchFilterRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/courses/list")
                        .param("pageNo", "2")
                        .param("pageSize", "5")
                        .param("sortBy", "title:asc")
                        .param("title", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.pageNo").value(2))
                .andExpect(jsonPath("$.result.items[0].id").value(3));

        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        var filterCaptor = org.mockito.ArgumentCaptor.forClass(CourseSearchFilterRequest.class);
        verify(courseService).searchCourses(pageableCaptor.capture(), filterCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(filterCaptor.getValue().title()).isEqualTo("Java");
    }

    @Test
    void generateVideoUrl_withValidRequest_returnsPresignedUrl() throws Exception {
        when(awsS3Service.generateVideoUploadUrl("lesson.mp4", 1024L))
                .thenReturn(PresignedUploadResponse.builder()
                        .uploadUrl("https://upload.example/lesson")
                        .fileUrl("courses/lesson.mp4")
                        .build());

        mockMvc.perform(post("/courses/videos/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PresignedUploadRequest.builder()
                                        .fileName("lesson.mp4")
                                        .fileSize(1024L)
                                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.uploadUrl").value("https://upload.example/lesson"));
    }

    @Test
    void generateVideoUrl_withZeroFileSize_returnsValidationError() throws Exception {
        mockMvc.perform(post("/courses/videos/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PresignedUploadRequest.builder()
                                        .fileName("lesson.mp4")
                                        .fileSize(0L)
                                        .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FILE_SIZE.getCode()));
    }

    @Test
    void softDeleteCourse_callsService() throws Exception {
        doNothing().when(courseService).softDeleteCourse(3L);

        mockMvc.perform(delete("/courses/{courseId}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Soft-delete course successfully"));

        verify(courseService).softDeleteCourse(3L);
    }

    private static CourseResponse course(Long id) {
        return CourseResponse.builder()
                .id(id)
                .title("Java Backend")
                .author("Nguyễn Văn A")
                .points(100L)
                .build();
    }
}
