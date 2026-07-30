package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.ForgotPasswordRequest;
import com.dxh.learninghub.dto.request.ResetPasswordRequest;
import com.dxh.learninghub.dto.request.UserCreationRequest;
import com.dxh.learninghub.dto.request.UserUpdateRequest;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.dto.response.UserUpdateResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponse createUser(UserCreationRequest request);

    void verifyRegister(String secretKey);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserResponse getMyInfo();

    UserUpdateResponse updateMyUser(UserUpdateRequest request, MultipartFile file);

    void resendVerification(String email);
}
