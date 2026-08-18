package com.dxh.learninghub.service;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.service.impl.TurnstileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TurnstileServiceImplTest {

    private static final String SITEVERIFY_URL =
            "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private MockRestServiceServer server;
    private TurnstileServiceImpl service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new TurnstileServiceImpl(
                builder,
                SITEVERIFY_URL,
                "test-secret",
                "learninghub.id.vn,localhost");
    }

    @Test
    void verify_acceptsValidTokenForExpectedActionAndHostname() {
        expectSiteverifyResponse("""
                {
                  "success": true,
                  "hostname": "learninghub.id.vn",
                  "action": "login",
                  "error-codes": []
                }
                """);

        assertThatCode(() -> service.verify("valid-token", "login"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void verify_rejectsTokenForDifferentAction() {
        expectSiteverifyResponse("""
                {
                  "success": true,
                  "hostname": "learninghub.id.vn",
                  "action": "register",
                  "error-codes": []
                }
                """);

        assertTurnstileError(() -> service.verify("valid-token", "login"));
        server.verify();
    }

    @Test
    void verify_rejectsHostnameOutsideAllowlist() {
        expectSiteverifyResponse("""
                {
                  "success": true,
                  "hostname": "untrusted.example",
                  "action": "login",
                  "error-codes": []
                }
                """);

        assertTurnstileError(() -> service.verify("valid-token", "login"));
        server.verify();
    }

    private void expectSiteverifyResponse(String responseBody) {
        server.expect(requestTo(SITEVERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("secret=test-secret")))
                .andExpect(content().string(containsString("response=valid-token")))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private static void assertTurnstileError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.TURNSTILE_VERIFICATION_FAILED));
    }
}
