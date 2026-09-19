package com.dxh.learninghub.utils;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificatePdfGenerator {

    static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    TemplateEngine templateEngine;

    public byte[] generate(String recipient, String courseName, String author,
                           LocalDate issueDate, String verificationCode) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Context context = new Context(Locale.ENGLISH);
            context.setVariable("recipient", recipient);
            context.setVariable("courseName", courseName);
            context.setVariable("author", author);
            context.setVariable("issueDate", DATE_FORMAT.format(issueDate));
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("verificationPath", "/api/v1/certificates/verify/" + verificationCode);

            PdfRendererBuilder builder = new PdfRendererBuilder();

            // font-weight: normal / 400
            try (InputStream isNormal = new ClassPathResource("fonts/font_inter.ttf").getInputStream()) {
                builder.useFont(() -> isNormal, "CertificateFont", 400, PdfRendererBuilder.FontStyle.NORMAL, true);
            }

            // font-weight: bold / 700
            try (InputStream isBold = new ClassPathResource("fonts/font_inter.ttf").getInputStream()) {
                builder.useFont(() -> isBold, "CertificateFont", 700, PdfRendererBuilder.FontStyle.NORMAL, true);
            }

            // Render PDF từ template HTML
            builder.withHtmlContent(templateEngine.process("certificate-template", context), null);
            builder.toStream(output);
            builder.run();

            return output.toByteArray();
        } catch (Exception exception) {
            throw new AppException(ErrorCode.CERTIFICATE_GENERATION_FAILED);
        }
    }
}