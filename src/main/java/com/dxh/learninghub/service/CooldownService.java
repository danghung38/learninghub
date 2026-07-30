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


@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CooldownService {

    StringRedisTemplate redis;

    public void checkCooldown(String action, String key, long seconds) {

        String redisKey = "cd:" + action + ":" + key;

        //chưa có sẽ tự lưu
        Boolean exists = redis.hasKey(redisKey);

        if (Boolean.TRUE.equals(exists)) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }

        redis.opsForValue().set(redisKey, "1", Duration.ofSeconds(seconds));
    }
}
