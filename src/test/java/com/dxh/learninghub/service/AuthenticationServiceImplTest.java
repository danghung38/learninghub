package com.dxh.learninghub.service;

import com.dxh.learninghub.dto.request.AuthenticationRequest;
import com.dxh.learninghub.dto.request.ChangePasswordRequest;
import com.dxh.learninghub.dto.response.AuthenticationResponse;
import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.RedisVerificationTokenRepository;
import com.dxh.learninghub.repo.RoleRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.impl.AuthenticationServiceImpl;
import com.dxh.learninghub.utils.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock LoginAttemptService loginAttemptService;
    @Mock CurrentUserProvider currentUserProvider;
    @InjectMocks AuthenticationServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "SIGNER_KEY",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(service, "VALID_DURATION", 1L);
        ReflectionTestUtils.setField(service, "REFRESHABLE_DURATION", 24L);
    }

    @Test
    void login_returnsTokensAndUpdatesLastLogin() {
        User user = activeUser();
        when(userRepository.findByUsernameOrEmail("student", "student")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded")).thenReturn(true);

        AuthenticationResponse response = service.login(
                AuthenticationRequest.builder().username("student").password("secret123").build(), "127.0.0.1");

        assertThat(response.authenticated()).isTrue();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.role()).isEqualTo(RoleEnum.USER.name());
        assertThat(user.getLastLogin()).isNotNull();
        verify(loginAttemptService).loginSucceeded("student", "127.0.0.1");
        verify(userRepository).save(user);
    }

    @Test
    void login_invalidPasswordRecordsFailedAttempt() {
        User user = activeUser();
        when(userRepository.findByUsernameOrEmail("student", "student")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertError(() -> service.login(
                AuthenticationRequest.builder().username("student").password("wrong").build(), "127.0.0.1"),
                ErrorCode.INVALID_CREDENTIALS);
        verify(loginAttemptService).loginFailed("student", "127.0.0.1");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_rejectsBannedAccountAfterCredentialCheck() {
        User user = activeUser();
        user.setBanned(true);
        when(userRepository.findByUsernameOrEmail("student", "student")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded")).thenReturn(true);

        assertError(() -> service.login(
                AuthenticationRequest.builder().username("student").password("secret123").build(), "127.0.0.1"),
                ErrorCode.ACCOUNT_BANNED);
        verify(loginAttemptService, never()).loginSucceeded(anyString(), anyString());
    }

    @Test
    void changePassword_rejectsIncorrectCurrentPassword() {
        User user = activeUser();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("bad-old", "encoded")).thenReturn(false);

        assertError(() -> service.changePassword(
                new ChangePasswordRequest("bad-old", "newSecret", "newSecret")),
                ErrorCode.CURRENT_PASSWORD_INCORRECT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_encodesAndPersistsNewPassword() {
        User user = activeUser();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("oldSecret", "encoded")).thenReturn(true);
        when(passwordEncoder.matches("newSecret", "encoded")).thenReturn(false);
        when(passwordEncoder.encode("newSecret")).thenReturn("new-encoded");

        service.changePassword(new ChangePasswordRequest("oldSecret", "newSecret", "newSecret"));

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        verify(userRepository).save(user);
    }

    private static User activeUser() {
        User user = User.builder().username("student").fullName("Student")
                .password("encoded").enabled(true).banned(false)
                .roles(Set.of(Role.builder().name(RoleEnum.USER.name()).build())).build();
        user.setId(5L);
        return user;
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                                    ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }
}
