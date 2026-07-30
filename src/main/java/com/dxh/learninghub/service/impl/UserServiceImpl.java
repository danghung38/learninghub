package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.ForgotPasswordRequest;
import com.dxh.learninghub.dto.request.ResetPasswordRequest;
import com.dxh.learninghub.dto.request.UserCreationRequest;
import com.dxh.learninghub.dto.request.UserUpdateRequest;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.dto.response.UserUpdateResponse;
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
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.CooldownService;
import com.dxh.learninghub.service.EmailService;
import com.dxh.learninghub.service.interfac.UserService;
import com.dxh.learninghub.utils.storage.FileUploadUtil;
import com.dxh.learninghub.utils.storage.UploadPolicy;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {
    static long VERIFY_TTL_SECONDS = 30 * 60L;
    static final SecureRandom SECURE_RANDOM = new SecureRandom();
    static final int MAX_OTP_ATTEMPTS = 10;

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;
    UserMapper userMapper;
    EmailService emailService;
    RedisVerificationTokenRepository redisVrRepository;
    AwsS3Service awsS3Service;
    CurrentUserProvider currentUserProvider;
    CooldownService cooldownService;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }

        Role userRole = roleRepository.findByName(RoleEnum.USER.name())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(false);
        user.setRoles(Set.of(userRole));

        User savedUser;

        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        sendVerificationToken(
                savedUser,
                request.email(),
                request.fullName()
        );

        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    @Override
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getEnabled()) throw new AppException(ErrorCode.ALREADY_VERIFIED);

        // COOLDOWN CHECK (60 giây)
        cooldownService.checkCooldown(
                "resend-verification",
                String.valueOf(user.getId()),
                60
        );

        // 1. Tìm các token cũ của user này
        List<RedisVerificationToken> oldTokens = redisVrRepository
                .findByUserIdAndVerifyType(user.getId(), VerifyType.REGISTER);

        // 2. Xóa chính xác từng entity tìm được
        if (!oldTokens.isEmpty()) {
            redisVrRepository.deleteAll(oldTokens);
        }

        // Gửi token mới
        sendVerificationToken(user, email, user.getFullName());
    }

    @Override
    @Transactional
    public void verifyRegister(String otp) {

        // Tìm thẳng theo ID (secretKey là @Id)
        RedisVerificationToken vt = redisVrRepository.findById(otp)
                .filter(t -> t.getVerifyType() == VerifyType.REGISTER)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_VERIFY_KEY));


        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getEnabled()) {
            throw new AppException(ErrorCode.ALREADY_VERIFIED);
        }

        user.setEnabled(true);
        userRepository.save(user);

        redisVrRepository.delete(vt);
    }




    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email())
                .filter(User::getEnabled)
                .ifPresent(user -> {
                    // 1. CHECK COOLDOWN (Chặn gửi liên tục trong 60s theo UserId/Email)
                    cooldownService.checkCooldown(
                            "forgot-password",
                            String.valueOf(user.getId()),
                            60
                    );

                    // 1. Tìm chính xác các OTP reset password cũ của user này
                    List<RedisVerificationToken> oldTokens = redisVrRepository
                            .findByUserIdAndVerifyType(user.getId(), VerifyType.RESET_PASSWORD);

                    // 2. Dọn sạch khỏi Redis trước khi tạo mới
                    if (!oldTokens.isEmpty()) {
                        redisVrRepository.deleteAll(oldTokens);
                    }

                    // tạo OTP mới
                    String resetCode = generateUniqueOtp();

                    redisVrRepository.save(RedisVerificationToken.builder()
                            .secretKey(resetCode)
                            .userId(user.getId())
                            .verifyType(VerifyType.RESET_PASSWORD)
                            .ttl(VERIFY_TTL_SECONDS)
                            .build());

                    emailService.sendResetPasswordEmail(user.getEmail(), user.getFullName(), resetCode);
                });

        // luôn trả OK dù email đúng hay sai
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // Tìm thẳng theo ID (secretKey là @Id)
        RedisVerificationToken vt = redisVrRepository.findById(request.resetCode())
                .filter(t -> t.getVerifyType() == VerifyType.RESET_PASSWORD)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_VERIFY_KEY));

        // Không cần check expiryDate — Redis TTL tự lo
        User user = userRepository.findById(vt.getUserId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        redisVrRepository.delete(vt);
    }


    @Override
    @Transactional
    public UserUpdateResponse updateMyUser(UserUpdateRequest request, MultipartFile file) {
        FileUploadUtil.validateIfPresent(file, UploadPolicy.AVATAR);
        User currentUser = currentUserProvider.getCurrentUser();
        User user = userRepository.findByIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // xử lý toàn bộ partial update (bỏ qua null)
        userMapper.updateUserFromRequest(request, user);

        String oldAvatar = null;
        if (file != null && !file.isEmpty()) {
            oldAvatar = user.getAvatar();
            awsS3Service.uploadFile(file, "avatars/" + user.getId(), UploadPolicy.AVATAR);
        }

        UserUpdateResponse response = userMapper.toUserUpdateResponse(userRepository.save(user));

        if (oldAvatar != null) {
            awsS3Service.deleteFileFromS3(oldAvatar);
        }

        return response;
    }

    // dùng chung cho createUser và resendVerification
    private void sendVerificationToken(User user, String email, String fullName) {
        String secretCode = generateUniqueOtp();

        redisVrRepository.save(RedisVerificationToken.builder()
                .secretKey(secretCode).userId(user.getId())
                .verifyType(VerifyType.REGISTER)
                .ttl(VERIFY_TTL_SECONDS).build());

        emailService.sendVerificationEmail(email, fullName, secretCode);
    }


    private String generateUniqueOtp() {
        for (int attempt = 0; attempt < MAX_OTP_ATTEMPTS; attempt++) {
            String otp = String.format("%08d", SECURE_RANDOM.nextInt(100_000_000));

            if (!redisVrRepository.existsById(otp)) {
                return otp;
            }
        }

        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @Override
    public UserResponse getMyInfo() {
        return userMapper.toUserResponse(currentUserProvider.getCurrentUser());
    }

}
