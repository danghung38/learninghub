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
                "subject",     "Xác thực tài khoản - Learning Hub",
                "htmlContent", buildOtpHtml(name, otp)
        );
        send(body, "OTP");
    }

    public void sendResetPasswordEmail(String to, String name, String resetCode) {
        Map<String, Object> body = Map.of(
                "sender",      Map.of("email", fromEmail, "name", fromName),
                "to",          List.of(Map.of("email", to)),
                "subject",     "Khôi phục mật khẩu - Learning Hub",
                "htmlContent", buildResetPasswordHtml(name, resetCode)
        );
        send(body, "ResetPassword");
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
            <html lang="vi">
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
                        <h2 style="color:#1a202c;margin:0 0 8px;">Xin chào, {{name}}! 👋</h2>
                        <p style="color:#718096;font-size:15px;line-height:1.6;margin:0 0 28px;">
                          Vui lòng nhập mã OTP bên dưới để xác thực tài khoản của bạn.
                        </p>
                        <div style="background:#f7f5ff;border:2px dashed #764ba2;border-radius:12px;
                                    padding:28px;text-align:center;margin-bottom:24px;">
                          <p style="margin:0 0 8px;color:#9a8abf;font-size:12px;
                                    text-transform:uppercase;letter-spacing:2px;">Mã xác thực</p>
                          <div style="font-size:42px;font-weight:800;letter-spacing:12px;
                                      color:#667eea;font-family:'Courier New',monospace;">{{otp}}</div>
                        </div>
                        <div style="background:#fff8e1;border-left:4px solid #f6ad55;
                                    border-radius:6px;padding:14px 18px;">
                          <p style="margin:0;color:#92400e;font-size:14px;">
                            ⏱ Mã có hiệu lực trong <strong>30 phút</strong>.
                            Không chia sẻ mã này với bất kỳ ai.
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
            <html lang="vi">
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
                        <h2 style="color:#1a202c;margin:0 0 8px;">Khôi phục mật khẩu</h2>
                        <p style="color:#718096;font-size:15px;line-height:1.6;margin:0 0 28px;">
                          Xin chào <strong>{{name}}</strong>, đây là mã xác nhận đặt lại mật khẩu của bạn.
                        </p>
                        <div style="background:#fff5f5;border:2px dashed #f5576c;border-radius:12px;
                                    padding:28px;text-align:center;margin-bottom:24px;">
                          <p style="margin:0 0 8px;color:#c53030;font-size:12px;
                                    text-transform:uppercase;letter-spacing:2px;">Mã xác nhận</p>
                          <div style="font-size:42px;font-weight:800;letter-spacing:12px;
                                      color:#f5576c;font-family:'Courier New',monospace;">{{resetCode}}</div>
                        </div>
                        <div style="background:#fff8e1;border-left:4px solid #f6ad55;
                                    border-radius:6px;padding:14px 18px;margin-bottom:12px;">
                          <p style="margin:0;color:#92400e;font-size:14px;">
                            ⏱ Mã có hiệu lực trong <strong>30 phút</strong>.
                          </p>
                        </div>
                        <div style="background:#ebf8ff;border-left:4px solid #63b3ed;
                                    border-radius:6px;padding:14px 18px;">
                          <p style="margin:0;color:#2c5282;font-size:14px;">
                            🛡 Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
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
}