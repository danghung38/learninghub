package com.dxh.learninghub.service.impl.admin;

import com.dxh.learninghub.dto.request.AdminResetPasswordRequest;
import com.dxh.learninghub.dto.request.UserSearchFilterRequest;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.UserMapper;
import com.dxh.learninghub.repo.RoleRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.repo.specification.UserSpecification;
import com.dxh.learninghub.service.interfac.admin.AdminUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void banUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (user.getBanned()) throw new AppException(ErrorCode.USER_ALREADY_BANNED);
        user.setBanned(true);
        userRepository.save(user);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (!user.getBanned()) throw new AppException(ErrorCode.USER_NOT_BANNED);
        user.setBanned(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void addRole(Long userId, String roleName) {
        RoleEnum roleEnum = parseRole(roleName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Role role = roleRepository.findByName(roleEnum.name())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        boolean alreadyHasRole = user.getRoles().stream()
                .anyMatch(existingRole -> existingRole.getName().equalsIgnoreCase(role.getName()));

        if (alreadyHasRole) throw new AppException(ErrorCode.USER_ALREADY_HAS_ROLE);
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void removeRole(Long userId, String roleName) {
        RoleEnum roleEnum = parseRole(roleName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Role roleToRemove = user.getRoles().stream()
                .filter(role -> role.getName().equalsIgnoreCase(roleEnum.name()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.USER_DOES_NOT_HAVE_ROLE));

        if (user.getRoles().size() <= 1) throw new AppException(ErrorCode.USER_MUST_HAVE_AT_LEAST_ONE_ROLE);

        user.getRoles().remove(roleToRemove);
        userRepository.save(user);
    }

    private RoleEnum parseRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new AppException(ErrorCode.ROLE_NOT_EXISTED);
        }

        try {
            return RoleEnum.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.ROLE_NOT_EXISTED);
        }
    }

    @Transactional
    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void resetPasswordByAdmin(Long userId, AdminResetPasswordRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PageResponse<UserResponse> searchUsers(Pageable pageable, UserSearchFilterRequest filter) {
        Specification<User> spec = UserSpecification.getUsersWithFilter(filter);
        Page<User> users = userRepository.findAll(spec, pageable);
        return toPageResponse(users);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PageResponse<UserResponse> getPendingTeacherApplications(Pageable pageable) {
        return toPageResponse(userRepository.findPendingTeacherApplications(pageable));
    }

    private PageResponse<UserResponse> toPageResponse(Page<User> page) {
        // Nhờ có @BatchSize(size = 20) không bị N+1
        List<UserResponse> responses = page.stream()
                .map(userMapper::toUserResponse)
                .toList();

        return PageResponse.<UserResponse>builder()
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPage(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .items(responses)
                .build();
    }

}
