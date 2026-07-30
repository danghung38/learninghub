package com.dxh.learninghub.configuration;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    private static final Logger log = LoggerFactory.getLogger(AuditorAwareImpl.class);

    @Override
    public Optional<String> getCurrentAuditor() {
        System.out.println("Current Auditor");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        return Optional.of(authentication.getName());
    }
}
