package com.dxh.learninghub.service.interfac.admin;

import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;


public interface AdminUserService {
    UserResponse getUserById(Long userId);

    PageResponse<UserResponse> searchUsers(
            Pageable pageable,
            String username,
            String fullName,
            String role,
            Boolean banned,
            Boolean enabled);

    PageResponse<UserResponse> getPendingTeacherApplications(Pageable pageable);
    void banUser(Long userId);
    void unbanUser(Long userId);

    void addRole(Long userId, String roleName);

    void removeRole(Long userId, String roleName);

}
