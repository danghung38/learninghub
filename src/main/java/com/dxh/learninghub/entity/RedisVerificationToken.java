package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.VerifyType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@RedisHash("verification_token")
public class RedisVerificationToken implements Serializable {

    @Id
    String secretKey;      // key = verification_token:<secretKey>


    @Indexed
    Long userId;           // để tìm user sau khi verify

    @Indexed
    VerifyType verifyType; // REGISTER hoặc RESET_PASSWORD

    @TimeToLive(unit = TimeUnit.SECONDS)
    Long ttl;              // 30 phút = 1800 giây, tự xóa
}