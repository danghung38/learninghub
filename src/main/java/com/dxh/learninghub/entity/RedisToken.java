package com.dxh.learninghub.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@RedisHash("blacklisted_token")  // tên hash trong Redis
public class RedisToken implements Serializable {

    @Id
    String id;

    @TimeToLive(unit = TimeUnit.SECONDS)
    Long ttl;
}