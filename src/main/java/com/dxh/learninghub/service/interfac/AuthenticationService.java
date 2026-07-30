package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.*;
import com.dxh.learninghub.dto.response.AuthenticationResponse;
import com.dxh.learninghub.dto.response.IntrospectResponse;
import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

public interface AuthenticationService {
    IntrospectResponse introspect(IntrospectRequest request)
                throws JOSEException, ParseException;

    AuthenticationResponse login(AuthenticationRequest request, String ip);

    AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException;

    void createPassword(CreatePasswordRequest request);
    void changePassword(ChangePasswordRequest request);

    void logout(LogoutRequest request) throws ParseException, JOSEException;
    AuthenticationResponse loginWithGoogle(GoogleLoginRequest request);
}
