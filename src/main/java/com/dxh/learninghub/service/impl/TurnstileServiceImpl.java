package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.interfac.TurnstileService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TurnstileServiceImpl implements TurnstileService {
    private final RestClient restClient;
    private final String secretKey;
    private final Set<String> allowedHostnames;

    public TurnstileServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${turnstile.siteverify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}") String siteverifyUrl,
            @Value("${turnstile.secret-key}") String secretKey,
            @Value("${turnstile.allowed-hostnames}") String allowedHostnames) {
        this.restClient = restClientBuilder.baseUrl(siteverifyUrl).build();
        this.secretKey = secretKey;
        this.allowedHostnames = Arrays.stream(allowedHostnames.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(hostname -> hostname.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void verify(String token, String expectedAction) {
        if (!StringUtils.hasText(token)) {
            throw new AppException(ErrorCode.TURNSTILE_VERIFICATION_FAILED);
        }

        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("secret", secretKey);
            form.add("response", token);

            TurnstileResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileResponse.class);

            boolean valid = response != null
                    && response.success()
                    && expectedAction.equals(response.action())
                    && isAllowedHostname(response.hostname());

            if (!valid) {
                log.warn("Turnstile verification rejected: action={}, hostname={}, errors={}",
                        response == null ? null : response.action(),
                        response == null ? null : response.hostname(),
                        response == null ? List.of() : response.errorCodes());
                throw new AppException(ErrorCode.TURNSTILE_VERIFICATION_FAILED);
            }
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.error("Turnstile Siteverify request failed", exception);
            throw new AppException(ErrorCode.TURNSTILE_VERIFICATION_FAILED);
        }
    }

    private boolean isAllowedHostname(String hostname) {
        return StringUtils.hasText(hostname)
                && allowedHostnames.contains(hostname.toLowerCase(Locale.ROOT));
    }

    private record TurnstileResponse(
            boolean success,
            String hostname,
            String action,
            @JsonProperty("error-codes") List<String> errorCodes) {
    }
}
