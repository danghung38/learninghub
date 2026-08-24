package com.dxh.learninghub.service.impl;


import com.dxh.learninghub.dto.request.*;
import com.dxh.learninghub.dto.response.AuthenticationResponse;
import com.dxh.learninghub.dto.response.IntrospectResponse;
import com.dxh.learninghub.entity.RedisVerificationToken;
import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.enums.TokenType;
import com.dxh.learninghub.enums.VerifyType;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.RedisVerificationTokenRepository;
import com.dxh.learninghub.repo.RoleRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.LoginAttemptService;
import com.dxh.learninghub.service.TokenBlacklistService;
import com.dxh.learninghub.service.interfac.TurnstileService;
import com.dxh.learninghub.service.interfac.AuthenticationService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    TokenBlacklistService tokenBlacklistService;
    PasswordEncoder passwordEncoder;
    LoginAttemptService loginAttemptService;
    CurrentUserProvider currentUserProvider;
    RedisVerificationTokenRepository redisVrRepository;
    TurnstileService turnstileService;


    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    protected String GOOGLE_CLIENT_ID;


    // introspect — dùng TokenType.ACCESS
    @Override
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        boolean isValid = true;
        try {
            SignedJWT signedJWT = verifyToken(request.token(), TokenType.ACCESS);
            findActiveUser(signedJWT.getJWTClaimsSet().getSubject());
        } catch (AppException e) {
            isValid = false;
        }
        return IntrospectResponse.builder().valid(isValid).build();
    }


    //    login
    @Override
    public AuthenticationResponse login(AuthenticationRequest request, String ip) {
        turnstileService.verify(request.turnstileToken(), "login");
        loginAttemptService.assertNotBlocked(request.username(), ip);

        User user = userRepository
                .findByUsernameOrEmail(request.username(), request.username())
                .orElse(null);

        boolean invalidCredentials = user == null
                        || user.getPassword() == null
                        || !passwordEncoder.matches(request.password(), user.getPassword());

        if (invalidCredentials) {
            loginAttemptService.loginFailed(request.username(), ip);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.getEnabled()) {throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);}
        if (user.getBanned()) {throw new AppException(ErrorCode.ACCOUNT_BANNED);}

        loginAttemptService.loginSucceeded(request.username(), ip);

        //last login
        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        String accessToken = generateToken(user, TokenType.ACCESS);
        String refreshToken = generateToken(user, TokenType.REFRESH);

        String roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.joining(" "));

        log.info("User {} login successful ip {}", request.username(), ip);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .role(roles)
                .fullName(user.getFullName())
                .username(user.getUsername())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserProvider.getCurrentUser();

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new AppException(ErrorCode.PASSWORD_NOT_CREATED);
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_MUST_BE_DIFFERENT);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        userRepository.save(user);
    }


    @Override
    @Transactional
    public void createPassword(CreatePasswordRequest request) {
        User user = currentUserProvider.getCurrentUser();

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            throw new AppException(ErrorCode.PASSWORD_ALREADY_CREATED);
        }

        if (!request.password().equals(request.confirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }

        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public AuthenticationResponse loginWithGoogle(GoogleLoginRequest request) {
        // 1. Verify idToken với Google
        GoogleIdToken.Payload payload = verifyGoogleToken(request.idToken());

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String fullName = (String) payload.get("name");
        String avatar = (String) payload.get("picture");

        // 2. Tìm user theo googleId -> email -> tạo mới
        User user = userRepository.findByGoogleId(googleId).orElseGet(() -> userRepository.findByEmail(email).map(existing -> {
            // Email đã tồn tại -> liên kết googleId vào tài khoản cũ
            existing.setGoogleId(googleId);
            if (existing.getAvatar() == null && avatar != null) existing.setAvatar(avatar);
            //Nếu tài khoản cũ chưa enabled, kích hoạt ngay lập tức!
            if (!Boolean.TRUE.equals(existing.getEnabled())) {
                existing.setEnabled(true);
                // Dọn dẹp Token OTP đăng ký tồn đọng trong Redis (nếu có)
                List<RedisVerificationToken> pendingTokens = redisVrRepository
                        .findByUserIdAndVerifyType(existing.getId(), VerifyType.REGISTER);
                if (!pendingTokens.isEmpty()) redisVrRepository.deleteAll(pendingTokens);
                log.info("Auto-verified user email {} via Google Login", email);
            }
            return userRepository.save(existing);
        }).orElseGet(() -> createGoogleUser(googleId, email, fullName, avatar)));

        // 3. Kiểm tra tài khoản có bị ban không
        if (!user.getEnabled()) throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        if (user.getBanned()) throw new AppException(ErrorCode.ACCOUNT_BANNED);

        // last login
        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        // 4. Tạo JWT app và trả về
        String accessToken = generateToken(user, TokenType.ACCESS);
        String refreshToken = generateToken(user, TokenType.REFRESH);
        String roles = user.getRoles().stream().map(Role::getName).collect(Collectors.joining(" "));

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .role(roles).fullName(user.getFullName())
                .username(user.getUsername()).build();
    }

    private User createGoogleUser(String googleId, String email, String fullName, String avatar) {
        Role userRole = roleRepository.findByName(RoleEnum.USER.name())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

        User user = User.builder()
                .googleId(googleId)
                .email(email)
                .username(email)
                .password(null)
                .fullName(fullName != null && !fullName.isBlank() ? fullName : email)
                .avatar(avatar)
                .enabled(true)
                .banned(false)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        return userRepository.save(user);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance()).setAudience(Collections.singletonList(GOOGLE_CLIENT_ID)).build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);

            return idToken.getPayload();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification failed", e);
            throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
    }


    // generateToken dùng enum
    private String generateToken(User user, TokenType tokenType) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        long duration = tokenType == TokenType.ACCESS ? VALID_DURATION : REFRESHABLE_DURATION;

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("danghung.com")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.HOURS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("token_type", tokenType.name())  // "ACCESS" hoặc "REFRESH"
                .claim("scope", tokenType == TokenType.ACCESS ? buildScope(user) : "").build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    //build roles
    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());

                //duyệt list permission trong roles
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
                }
            });
        }


        return stringJoiner.toString();
    }


    // refreshToken
    @Override
    public AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException {

        var signedJWT = verifyToken(request.token(), TokenType.REFRESH);
        String jti = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        String username = signedJWT.getJWTClaimsSet().getSubject();
        User user = findActiveUser(username);

        tokenBlacklistService.blacklist(jti, expiryTime);

        return AuthenticationResponse.builder()
                .accessToken(generateToken(user, TokenType.ACCESS))
                .refreshToken(generateToken(user, TokenType.REFRESH))
                .authenticated(true)
                .build();
    }

    private User findActiveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }
        return user;
    }

    //redis
    @Override
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        // Blacklist access token
        try {
            var accessJwt = verifyToken(request.accessToken(), TokenType.ACCESS);
            tokenBlacklistService.blacklist(
                    accessJwt.getJWTClaimsSet().getJWTID(),
                    accessJwt.getJWTClaimsSet().getExpirationTime()
            );
        } catch (AppException e) {
            log.info("Access token already expired or invalid");
        }

        // Blacklist refresh token
        try {
            var refreshJwt = verifyToken(request.refreshToken(), TokenType.REFRESH);
            tokenBlacklistService.blacklist(
                    refreshJwt.getJWTClaimsSet().getJWTID(),
                    refreshJwt.getJWTClaimsSet().getExpirationTime()
            );
        } catch (AppException e) {
            log.info("Refresh token already expired or invalid");
        }
    }


    // verifyToken
    private SignedJWT verifyToken(String token, TokenType expectedType)
            throws JOSEException {

        final SignedJWT signedJWT;

        try {
            signedJWT = SignedJWT.parse(token);
        } catch (ParseException | RuntimeException exception) {
            log.debug("Invalid JWT format");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Chỉ chấp nhận đúng thuật toán hệ thống đang dùng
        if (!JWSAlgorithm.HS512.equals(signedJWT.getHeader().getAlgorithm())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes(StandardCharsets.UTF_8));

        // Verify chữ ký trước khi sử dụng claims
        if (!signedJWT.verify(verifier)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        try {
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            Date expiryTime = claims.getExpirationTime();
            if (expiryTime == null || !expiryTime.after(new Date())) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            if (!"danghung.com".equals(claims.getIssuer())) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String tokenTypeClaim = claims.getStringClaim("token_type");
            if (tokenTypeClaim == null) {
                throw new AppException(ErrorCode.INVALID_TOKEN_TYPE);
            }

            final TokenType actualType;
            try {
                actualType = TokenType.valueOf(tokenTypeClaim);
            } catch (IllegalArgumentException exception) {
                throw new AppException(ErrorCode.INVALID_TOKEN_TYPE);
            }

            if (actualType != expectedType) {
                throw new AppException(ErrorCode.INVALID_TOKEN_TYPE);
            }

            String jti = claims.getJWTID();
            if (jti == null || jti.isBlank() || tokenBlacklistService.isBlacklisted(jti)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            return signedJWT;

        } catch (ParseException exception) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}

