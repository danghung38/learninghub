package com.dxh.learninghub.utils;

import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurrentUserProvider {

    UserRepository userRepository;

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }
        return user;
    }
}
