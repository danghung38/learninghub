package com.dxh.learninghub.service;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificatePdfGenerator {

    static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static List<String> FONT_PATHS = List.of(
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf");

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
            findFont().ifPresent(font -> builder.useFont(font, "CertificateFont"));
            builder.withHtmlContent(templateEngine.process("certificate-template", context), null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new AppException(ErrorCode.CERTIFICATE_GENERATION_FAILED);
        }
    }

    private java.util.Optional<File> findFont() {
        return FONT_PATHS.stream().map(File::new).filter(File::isFile).findFirst();
    }
}
