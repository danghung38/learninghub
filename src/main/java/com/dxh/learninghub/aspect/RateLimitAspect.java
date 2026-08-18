package com.dxh.learninghub.aspect;


import com.dxh.learninghub.service.RateLimitService;
import com.dxh.learninghub.validator.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final HttpServletRequest request;

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(RateLimit rateLimit) {

        String ip = request.getRemoteAddr();

        rateLimitService.checkLimit(
                rateLimit.action().name(),
                ip,
                rateLimit.maxRequests(),
                Duration.ofMinutes(rateLimit.durationMinutes())
        );
    }
}