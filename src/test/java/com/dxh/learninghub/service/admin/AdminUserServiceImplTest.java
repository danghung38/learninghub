package com.dxh.learninghub.service.admin;

import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.UserMapper;
import com.dxh.learninghub.repo.RoleRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.impl.admin.AdminUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserMapper userMapper;

    @InjectMocks AdminUserServiceImpl adminUserService;

    @Test
    void banUser_activeUser_marksUserAsBanned() {
        User user = userWithRoles("USER");
        user.setBanned(false);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        adminUserService.banUser(3L);

        assertThat(user.getBanned()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void banUser_alreadyBanned_returnsBusinessError() {
        User user = userWithRoles("USER");
        user.setBanned(true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        assertError(() -> adminUserService.banUser(3L), ErrorCode.USER_ALREADY_BANNED);
        verify(userRepository, never()).save(user);
    }

    @Test
    void addRole_newRole_addsRoleOnce() {
        User user = userWithRoles("USER");
        Role teacher = Role.builder().name("TEACHER").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacher));

        adminUserService.addRole(3L, "teacher");

        assertThat(user.getRoles()).extracting(Role::getName)
                .containsExactlyInAnyOrder("USER", "TEACHER");
        verify(userRepository).save(user);
    }

    @Test
    void removeRole_lastRole_isRejected() {
        User user = userWithRoles("USER");
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        assertError(() -> adminUserService.removeRole(3L, "USER"),
                ErrorCode.USER_MUST_HAVE_AT_LEAST_ONE_ROLE);
    }

    @Test
    void addRole_invalidEnum_returnsRoleNotExisted() {
        assertError(() -> adminUserService.addRole(3L, "SUPER_ADMIN"),
                ErrorCode.ROLE_NOT_EXISTED);
        verify(userRepository, never()).findById(3L);
    }

    private static User userWithRoles(String... names) {
        Set<Role> roles = new LinkedHashSet<>();
        for (String name : names) roles.add(Role.builder().name(name).build());
        return User.builder().username("user").email("user@example.com").roles(roles).build();
    }

    private static void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(errorCode));
    }
}
