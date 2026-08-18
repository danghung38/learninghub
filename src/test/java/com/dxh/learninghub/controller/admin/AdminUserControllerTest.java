package com.dxh.learninghub.controller.admin;

import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.interfac.admin.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @Test
    void getUserById_returnsUserDetails() throws Exception {
        UserResponse user = user(17L);
        when(adminUserService.getUserById(17L)).thenReturn(user);

        mockMvc.perform(get("/admin/users/{userId}", 17L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Successfully get user details"))
                .andExpect(jsonPath("$.result.id").value(17))
                .andExpect(jsonPath("$.result.username").value("nguyenvana"))
                .andExpect(jsonPath("$.result.lastLogin").value("2026-08-17T10:20:30"));

        verify(adminUserService).getUserById(17L);
    }

    @Test
    void searchUsers_buildsPageableAndPassesFiltersToService() throws Exception {
        PageResponse<UserResponse> page = PageResponse.<UserResponse>builder()
                .pageNo(2)
                .pageSize(20)
                .totalPage(3)
                .totalElements(41)
                .items(List.of(user(17L)))
                .build();
        when(adminUserService.searchUsers(any(Pageable.class), any())).thenReturn(page);

        mockMvc.perform(get("/admin/users/list")
                        .param("pageNo", "2")
                        .param("pageSize", "20")
                        .param("sortBy", "fullName:asc")
                        .param("username", "nguyen")
                        .param("fullName", "Nguyễn Văn A")
                        .param("role", "USER")
                        .param("banned", "false")
                        .param("enabled", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Successfully get user list"))
                .andExpect(jsonPath("$.result.pageNo").value(2))
                .andExpect(jsonPath("$.result.pageSize").value(20))
                .andExpect(jsonPath("$.result.totalElements").value(41))
                .andExpect(jsonPath("$.result.items[0].id").value(17));

        var pageableCaptor = forClass(Pageable.class);
        var filterCaptor = forClass(
                com.dxh.learninghub.dto.request.UserSearchFilterRequest.class);
        verify(adminUserService).searchUsers(pageableCaptor.capture(), filterCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("fullName")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("fullName").isAscending()).isTrue();

        var filter = filterCaptor.getValue();
        assertThat(filter.username()).isEqualTo("nguyen");
        assertThat(filter.fullName()).isEqualTo("Nguyễn Văn A");
        assertThat(filter.role()).isEqualTo("USER");
        assertThat(filter.banned()).isFalse();
        assertThat(filter.enabled()).isTrue();
    }

    @Test
    void banUser_callsServiceAndReturnsSuccess() throws Exception {
        doNothing().when(adminUserService).banUser(17L);

        mockMvc.perform(post("/admin/users/ban/{userId}", 17L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User banned successfully"))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(adminUserService).banUser(17L);
    }

    @Test
    void unbanUser_callsServiceAndReturnsSuccess() throws Exception {
        doNothing().when(adminUserService).unbanUser(17L);

        mockMvc.perform(post("/admin/users/unban/{userId}", 17L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User unbanned successfully"));

        verify(adminUserService).unbanUser(17L);
    }

    @Test
    void addRole_callsServiceWithPathVariables() throws Exception {
        mockMvc.perform(post("/admin/users/{userId}/roles/{roleName}", 17L, "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Role added to user successfully"));

        verify(adminUserService).addRole(17L, "TEACHER");
    }

    @Test
    void removeRole_callsServiceWithPathVariables() throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}/roles/{roleName}", 17L, "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Role removed from user successfully"));

        verify(adminUserService).removeRole(17L, "TEACHER");
    }

    @Test
    void getUserById_whenUserDoesNotExist_returnsMappedError() throws Exception {
        when(adminUserService.getUserById(999L))
                .thenThrow(new AppException(ErrorCode.USER_NOT_EXISTED));

        mockMvc.perform(get("/admin/users/{userId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_EXISTED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_EXISTED.getMessage()));
    }

    private static UserResponse user(Long id) {
        return UserResponse.builder()
                .id(id)
                .username("nguyenvana")
                .fullName("Nguyễn Văn A")
                .email("nguyenvana@example.com")
                .lastLogin(LocalDateTime.of(2026, 8, 17, 10, 20, 30))
                .build();
    }
}
