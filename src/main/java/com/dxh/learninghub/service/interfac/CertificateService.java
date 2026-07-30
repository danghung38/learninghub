package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.response.CertificateVerificationResponse;

public interface CertificateService {

    String getOrCreateDownloadUrl(Long courseId);

    CertificateVerificationResponse verify(String verificationCode);
}
