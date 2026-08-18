package com.dxh.learninghub.service;

import com.dxh.learninghub.dto.request.UserCreationRequest;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.entity.RedisVerificationToken;
import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.enums.VerifyType;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.UserMapper;
import com.dxh.learninghub.repo.RedisVerificationTokenRepository;
import com.dxh.learninghub.repo.RoleRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.impl.UserServiceImpl;
import com.dxh.learninghub.utils.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RoleRepository roleRepository;
    @Mock UserMapper userMapper;
    @Mock EmailService emailService;
    @Mock RedisVerificationTokenRepository redisVrRepository;
    @Mock AwsS3Service awsS3Service;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock CooldownService cooldownService;
    @InjectMocks UserServiceImpl service;

    @Test
    void createUser_savesDisabledUserAndSendsOtp() {
        UserCreationRequest request = request();
        Role role = Role.builder().name(RoleEnum.USER.name()).build();
        User user = User.builder().username(request.username()).email(request.email()).build();
        user.setId(7L);
        UserResponse response = UserResponse.builder().id(7L).username(request.username()).build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleEnum.USER.name())).thenReturn(Optional.of(role));
        when(userMapper.toUser(request)).thenReturn(user);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(user)).thenReturn(user);
        when(redisVrRepository.existsById(anyString())).thenReturn(false);
        when(userMapper.toUserResponse(user)).thenReturn(response);

        assertThat(service.createUser(request)).isSameAs(response);
        assertThat(user.getEnabled()).isFalse();
        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.getRoles()).containsExactly(role);

        ArgumentCaptor<RedisVerificationToken> tokenCaptor =
                ArgumentCaptor.forClass(RedisVerificationToken.class);
        verify(redisVrRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(tokenCaptor.getValue().getVerifyType()).isEqualTo(VerifyType.REGISTER);
        assertThat(tokenCaptor.getValue().getSecretKey()).hasSize(8);
        verify(emailService).sendVerificationEmail(eq(request.email()), eq(request.fullName()), anyString());
    }

    @Test
    void createUser_rejectsExistingEmailBeforeWriting() {
        UserCreationRequest request = request();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User()));

        assertError(() -> service.createUser(request), ErrorCode.EMAIL_EXISTED);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void verifyRegister_enablesUserAndDeletesOtp() {
        RedisVerificationToken token = RedisVerificationToken.builder()
                .secretKey("12345678").userId(9L).verifyType(VerifyType.REGISTER).build();
        User user = User.builder().enabled(false).build();
        when(redisVrRepository.findById("12345678")).thenReturn(Optional.of(token));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        service.verifyRegister("12345678");

        assertThat(user.getEnabled()).isTrue();
        verify(userRepository).save(user);
        verify(redisVrRepository).delete(token);
    }

    @Test
    void verifyRegister_rejectsWrongTokenType() {
        RedisVerificationToken token = RedisVerificationToken.builder()
                .secretKey("12345678").userId(9L).verifyType(VerifyType.RESET_PASSWORD).build();
        when(redisVrRepository.findById("12345678")).thenReturn(Optional.of(token));

        assertError(() -> service.verifyRegister("12345678"), ErrorCode.INVALID_VERIFY_KEY);
        verifyNoInteractions(userRepository);
    }

    private static UserCreationRequest request() {
        return UserCreationRequest.builder()
                .username("student01").password("secret123").fullName("Student One")
                .phoneNumber("0912345678").email("student@example.com").address("Ha Noi")
                .gender("MALE").dob(LocalDate.of(2000, 1, 1)).build();
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                                    ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }
}
