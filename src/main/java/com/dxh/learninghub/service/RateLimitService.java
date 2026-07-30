package com.dxh.learninghub.service;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate limit dạng "fixed window" bằng Redis (INCR + EXPIRE).
 * Dùng cho register / forgot-password / reset-password / gửi OTP...
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;

    /**
     * @param action      tên hành động (vd "register", "forgot-password")
     * @param key         định danh giới hạn (IP hoặc email)
     * @param maxRequests số request tối đa trong cửa sổ
     * @param window      độ dài cửa sổ thời gian
     */
    public void checkLimit(String action, String key, int maxRequests, Duration window) {
        String redisKey = "rl:" + action + ":" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, window);
        }
        if (count != null && count > maxRequests) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
