package com.dxh.learninghub.configuration;

import com.dxh.learninghub.constant.CacheNames;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableCaching
@EnableRedisRepositories(basePackages = "com.dxh.learninghub.repo")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // key và value đều là String
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var valueSerializer = GenericJackson2JsonRedisSerializer.builder().build();
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
                .prefixCacheNameWith("learninghub::");

        Map<String, RedisCacheConfiguration> configurations = Map.of(
                CacheNames.COURSES, defaults.entryTtl(Duration.ofMinutes(15)),
                CacheNames.COURSE_LIST, defaults.entryTtl(Duration.ofMinutes(5)),
                CacheNames.COURSE_TITLE, defaults.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(configurations)
                .disableCreateOnMissingCache()
                .build();
    }

    @Bean("courseListKeyGenerator")
    KeyGenerator courseListKeyGenerator() {
        return (target, method, params) -> Arrays.deepToString(params);
    }
}
