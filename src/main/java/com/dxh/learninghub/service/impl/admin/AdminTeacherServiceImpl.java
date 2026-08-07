package com.dxh.learninghub.service.impl.admin;

import com.dxh.learninghub.dto.response.admin.TeacherApplicationDetailResponse;
import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RegistrationStatus;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.UserMapper;
import com.dxh.learninghub.repo.RoleRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.service.interfac.admin.AdminTeacherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminTeacherServiceImpl implements AdminTeacherService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TeacherApplicationDetailResponse getUserApplicationDetail(Long userId) {
        User user = findUserById(userId);

        return userMapper.toTeacherApplicationDetailResponse(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void approveTeacherRegistration(Long userId) {
        User user = findUserById(userId);

        if (user.getRegistrationStatus() != RegistrationStatus.PENDING) {
            throw new AppException(ErrorCode.REGISTRATION_NOT_PENDING);
        }

        Role teacherRole = roleRepository
                .findByName(RoleEnum.TEACHER.name())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        Set<Role> roles = getMutableRoles(user);

        boolean alreadyTeacher = roles.stream()
                .anyMatch(role -> RoleEnum.TEACHER.name().equalsIgnoreCase(role.getName()));

        if (!alreadyTeacher) roles.add(teacherRole);
        user.setRoles(roles);
        user.setRegistrationStatus(RegistrationStatus.APPROVED);

        notificationService.createNotification(
                user,
                null,
                "Teacher application approved",
                "Your teacher application has been approved",
                null);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void rejectTeacherRegistration(Long userId) {
        User user = findUserById(userId);

        if (user.getRegistrationStatus() != RegistrationStatus.PENDING) {
            throw new AppException(ErrorCode.REGISTRATION_NOT_PENDING);
        }

        user.setRegistrationStatus(RegistrationStatus.REJECTED);

        notificationService.createNotification(
                user,
                null,
                "Teacher application rejected",
                "Your teacher application has been rejected",
                null);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void removeTeacherRole(Long userId) {
        User user = findUserById(userId);

        Set<Role> roles = getMutableRoles(user);

        Role teacherRole = roles.stream()
                .filter(role -> RoleEnum.TEACHER.name().equalsIgnoreCase(role.getName()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_TEACHER));

        roles.remove(teacherRole);

        if (roles.isEmpty()) {
            Role userRole = roleRepository
                    .findByName(RoleEnum.USER.name())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
            roles.add(userRole);
        }

        user.setRoles(roles);
        user.setRegistrationStatus(RegistrationStatus.NONE);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Set<Role> getMutableRoles(User user) {
        if (user.getRoles() == null) return new HashSet<>();
        return new HashSet<>(user.getRoles());
    }
}
