package com.dxh.learninghub.service;

import com.dxh.learninghub.entity.RedisToken;
import com.dxh.learninghub.repo.RedisTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenBlacklistService {
    RedisTokenRepository redisTokenRepository;

    public void blacklist(String jti, Date expiryTime) {
        long ttlSeconds = (expiryTime.getTime() - System.currentTimeMillis()) / 1000;
        if (ttlSeconds <= 0) return; // token đã hết hạn, không cần lưu

        RedisToken entity = RedisToken.builder()
                .id(jti)
                .ttl(ttlSeconds)
                .build();

        redisTokenRepository.save(entity);
    }

    public boolean isBlacklisted(String jti) {
        return redisTokenRepository.existsById(jti);
    }
}
