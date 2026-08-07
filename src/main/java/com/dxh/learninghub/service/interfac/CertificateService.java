package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.response.CertificateVerificationResponse;
import com.dxh.learninghub.dto.response.CertificateResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface CertificateService {

    @Transactional(readOnly = true)
    PageResponse<CertificateResponse> getMyCertificates(Pageable pageable);

    @Transactional
    byte[] downloadCertificatePdf(Long courseId);

    CertificateVerificationResponse verify(String verificationCode);
}
