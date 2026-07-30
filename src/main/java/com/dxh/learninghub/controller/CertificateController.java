package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.response.ApiResponse;
import com.dxh.learninghub.dto.response.CertificateVerificationResponse;
import com.dxh.learninghub.service.interfac.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/certificates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Certificates", description = "APIs for downloading and verifying course certificates")
public class CertificateController {

    CertificateService certificateService;

    @Operation(summary = "Download a certificate", description = "Create the course certificate when needed and download the PDF file directly")
    @GetMapping("/courses/{courseId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long courseId) {
        byte[] pdfBytes = certificateService.downloadCertificatePdf(courseId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certificate-course-" + courseId + ".pdf\"")
                .body(pdfBytes);
    }
    @Operation(summary = "Verify a certificate", description = "Verify a certificate using its public verification code")
    @GetMapping("/verify/{verificationCode}")
    ApiResponse<CertificateVerificationResponse> verify(@PathVariable String verificationCode) {
        return ApiResponse.<CertificateVerificationResponse>builder()
                .message("Certificate is valid")
                .result(certificateService.verify(verificationCode))
                .build();
    }
}
