package com.dxh.learninghub.service;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-SERVICE")
public class EmailService {

    private final WebClient brevoWebClient;

    @Value("${spring.brevo.api-key}")
    private String apiKey;

    @Value("${spring.brevo.from-email}")
    private String fromEmail;

    @Value("${spring.brevo.from-name}")
    private String fromName;

    public void sendVerificationEmail(String to, String name, String otp) {
        Map<String, Object> body = Map.of(
                "sender",      Map.of("email", fromEmail, "name", fromName),
                "to",          List.of(Map.of("email", to)),
                "subject",     "Verify your account - Learning Hub",
                "htmlContent", buildOtpHtml(name, otp)
        );
        send(body, "OTP");
    }

    public void sendResetPasswordEmail(String to, String name, String resetCode) {
        Map<String, Object> body = Map.of(
                "sender",      Map.of("email", fromEmail, "name", fromName),
                "to",          List.of(Map.of("email", to)),
                "subject",     "Reset your password - Learning Hub",
                "htmlContent", buildResetPasswordHtml(name, resetCode)
        );
        send(body, "ResetPassword");
    }

    public void sendAdvertisementEmail(
            String to,
            String name,
            String title,
            String description,
            String link) {
        Map<String, Object> body = Map.of(
                "sender",      Map.of("email", fromEmail, "name", fromName),
                "to",          List.of(Map.of("email", to)),
                "subject",     title + " - Learning Hub",
                "htmlContent", buildAdvertisementHtml(name, title, description, link)
        );
        send(body, "Advertisement");
    }

    private void send(Map<String, Object> body, String type) {
        brevoWebClient.post()
                .uri("/smtp/email")
                .header("api-key", apiKey)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(err -> log.error("Brevo error [{}]: {}", type, err))
                                .then(Mono.error(new AppException(ErrorCode.SEND_FAILED)))
                )
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(v -> log.info("[{}] email sent successfully", type))
                .doOnError(e -> log.error("[{}] {}", type, e.getMessage()))
                .subscribe();
    }

    private String buildOtpHtml(String name, String otp) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"/></head>
            <body style="margin:0;padding:0;background:#f0f4f8;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                    style="background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#667eea,#764ba2);padding:40px;text-align:center;">
                        <div style="font-size:36px;">🎓</div>
                        <h1 style="color:#fff;margin:8px 0 0;font-size:24px;">Learning Hub</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="color:#1a202c;margin:0 0 8px;">Hello, {{name}}! 👋</h2>
                        <p style="color:#718096;font-size:15px;line-height:1.6;margin:0 0 28px;">
                          Enter the OTP below to verify your Learning Hub account.
                        </p>
                        <div style="background:#f7f5ff;border:2px dashed #764ba2;border-radius:12px;
                                    padding:28px;text-align:center;margin-bottom:24px;">
                          <p style="margin:0 0 8px;color:#9a8abf;font-size:12px;
                                    text-transform:uppercase;letter-spacing:2px;">Verification code</p>
                          <div style="font-size:42px;font-weight:800;letter-spacing:12px;
                                      color:#667eea;font-family:'Courier New',monospace;">{{otp}}</div>
                        </div>
                        <div style="background:#fff8e1;border-left:4px solid #f6ad55;
                                    border-radius:6px;padding:14px 18px;">
                          <p style="margin:0;color:#92400e;font-size:14px;">
                            ⏱ This code expires in <strong>30 minutes</strong>.
                            Never share it with anyone.
                          </p>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f7fafc;padding:20px 40px;border-top:1px solid #e2e8f0;text-align:center;">
                        <p style="margin:0;color:#cbd5e0;font-size:12px;">© 2026 Learning Hub</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """
                .replace("{{name}}", name)
                .replace("{{otp}}", otp);
    }

    private String buildResetPasswordHtml(String name, String resetCode) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"/></head>
            <body style="margin:0;padding:0;background:#f0f4f8;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                    style="background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#f093fb,#f5576c);padding:40px;text-align:center;">
                        <div style="font-size:36px;">🔐</div>
                        <h1 style="color:#fff;margin:8px 0 0;font-size:24px;">Learning Hub</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="color:#1a202c;margin:0 0 8px;">Reset your password</h2>
                        <p style="color:#718096;font-size:15px;line-height:1.6;margin:0 0 28px;">
                          Hello <strong>{{name}}</strong>, use this code to reset your password.
                        </p>
                        <div style="background:#fff5f5;border:2px dashed #f5576c;border-radius:12px;
                                    padding:28px;text-align:center;margin-bottom:24px;">
                          <p style="margin:0 0 8px;color:#c53030;font-size:12px;
                                    text-transform:uppercase;letter-spacing:2px;">Reset code</p>
                          <div style="font-size:42px;font-weight:800;letter-spacing:12px;
                                      color:#f5576c;font-family:'Courier New',monospace;">{{resetCode}}</div>
                        </div>
                        <div style="background:#fff8e1;border-left:4px solid #f6ad55;
                                    border-radius:6px;padding:14px 18px;margin-bottom:12px;">
                          <p style="margin:0;color:#92400e;font-size:14px;">
                            ⏱ This code expires in <strong>30 minutes</strong>.
                          </p>
                        </div>
                        <div style="background:#ebf8ff;border-left:4px solid #63b3ed;
                                    border-radius:6px;padding:14px 18px;">
                          <p style="margin:0;color:#2c5282;font-size:14px;">
                            🛡 If you did not request a password reset, you can safely ignore this email.
                          </p>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f7fafc;padding:20px 40px;border-top:1px solid #e2e8f0;text-align:center;">
                        <p style="margin:0;color:#cbd5e0;font-size:12px;">© 2026 Learning Hub</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """
                .replace("{{name}}", name)
                .replace("{{resetCode}}", resetCode);
    }

    private String buildAdvertisementHtml(
            String name,
            String title,
            String description,
            String link) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"/></head>
            <body style="margin:0;padding:0;background:#f4f5fb;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                    style="background:#fff;border-radius:18px;overflow:hidden;box-shadow:0 12px 40px rgba(53,45,145,.12);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#5145cd,#8b5cf6);padding:34px;text-align:center;">
                        <div style="font-size:36px;">&#128227;</div>
                        <h1 style="color:#fff;margin:8px 0 0;font-size:24px;">Learning Hub</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px;">
                        <p style="color:#6b63ca;font-size:12px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;margin:0 0 10px;">New announcement</p>
                        <h2 style="color:#172033;margin:0 0 12px;font-size:24px;">{{title}}</h2>
                        <p style="color:#667085;font-size:15px;line-height:1.7;margin:0 0 26px;">Hello {{name}}, {{description}}</p>
                        <a href="{{link}}" style="display:inline-block;background:#5749d6;color:#fff;text-decoration:none;border-radius:10px;padding:13px 21px;font-weight:700;">View announcement</a>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#fafaff;padding:18px 36px;border-top:1px solid #ececf5;text-align:center;">
                        <p style="margin:0;color:#98a2b3;font-size:12px;">You received this email from Learning Hub.</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """
                .replace("{{name}}", safe(name))
                .replace("{{title}}", safe(title))
                .replace("{{description}}", safe(description))
                .replace("{{link}}", safe(link));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
