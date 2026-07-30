package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.response.CertificateVerificationResponse;
import org.springframework.transaction.annotation.Transactional;

public interface CertificateService {


    @Transactional
    byte[] downloadCertificatePdf(Long courseId);

    CertificateVerificationResponse verify(String verificationCode);
}
