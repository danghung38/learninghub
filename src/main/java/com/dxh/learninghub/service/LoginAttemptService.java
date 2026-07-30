package com.dxh.learninghub.service;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Chống brute-force đăng nhập bằng Redis.
 * - Khoá theo CẢ username (định danh đăng nhập) VÀ IP.
 * - 5 lần đăng nhập sai → khoá 5 phút.
 *   + Hacker thử 100 email khác nhau từ 1 IP → IP bị khoá.
 *   + Hacker đổi IP liên tục để dò 1 tài khoản → tài khoản đó bị khoá.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginAttemptService {

    StringRedisTemplate redis;

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(5);

    private String userKey(String identifier) {
        return "login:fail:user:" + identifier;
    }

    private String ipKey(String ip) {
        return "login:fail:ip:" + ip;
    }

    /** Kiểm tra trước khi xử lý login — nếu đang bị khoá thì chặn luôn. */
    public void assertNotBlocked(String identifier, String ip) {
        if (count(userKey(identifier)) >= MAX_ATTEMPTS || count(ipKey(ip)) >= MAX_ATTEMPTS) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    /** Ghi nhận 1 lần đăng nhập sai cho cả username lẫn IP. */
    public void loginFailed(String identifier, String ip) {
        increment(userKey(identifier));
        increment(ipKey(ip));
    }

    /** Đăng nhập thành công → xoá bộ đếm để mở khoá. */
    public void loginSucceeded(String identifier, String ip) {
        redis.delete(userKey(identifier));
        redis.delete(ipKey(ip));
    }

    private long count(String key) {
        String v = redis.opsForValue().get(key);
        return v == null ? 0 : Long.parseLong(v);
    }

    private void increment(String key) {
        Long c = redis.opsForValue().increment(key);
        // Lần đầu tăng (c==1) → set TTL khoá. TTL không reset ở các lần sau (giữ nguyên thời gian khoá).
        if (c != null && c == 1L) {
            redis.expire(key, BLOCK_DURATION);
        }
    }
}
