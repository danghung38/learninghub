package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.response.CertificateVerificationResponse;
import com.dxh.learninghub.entity.Certificate;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.repo.CertificateRepository;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.service.CertificatePdfGenerator;
import com.dxh.learninghub.service.interfac.CertificateService;
import com.dxh.learninghub.service.interfac.LearningProgressService;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    CertificateRepository certificateRepository;
    CourseRepository courseRepository;
    LearningProgressService learningProgressService;
    CurrentUserProvider currentUserProvider;
    CertificatePdfGenerator pdfGenerator;

    @Override
    @Transactional
    public byte[] downloadCertificatePdf(Long courseId) {
        User user = currentUserProvider.getCurrentUser();
        Certificate certificate = certificateRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseGet(() -> createCertificate(user, courseId));

        // Render PDF trực tiếp từ dữ liệu trong DB
        return pdfGenerator.generate(
                certificate.getUser().getFullName(),
                certificate.getCourse().getTitle(),
                certificate.getCourse().getAuthor().getFullName(),
                certificate.getIssueDate(),
                certificate.getVerificationCode()
        );
    }


    @Override
    @Transactional(readOnly = true)
    public CertificateVerificationResponse verify(String verificationCode) {
        Certificate certificate = certificateRepository.findByVerificationCode(
                        verificationCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new AppException(ErrorCode.CERTIFICATE_NOT_EXISTED));

        return new CertificateVerificationResponse(
                certificate.getVerificationCode(),
                certificate.getUser().getFullName(),
                certificate.getCourse().getTitle(),
                certificate.getCourse().getAuthor().getFullName(),
                certificate.getIssueDate(),
                true);
    }

    private Certificate createCertificate(User user, Long courseId) {
        if (!Boolean.TRUE.equals(learningProgressService.getCourseProgress(courseId).completed())) {
            throw new AppException(ErrorCode.COURSE_NOT_COMPLETED);
        }

        Course course = courseRepository.findWithAuthorById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        String verificationCode = "LH-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);

        // Chỉ save metadata vào DB
        return certificateRepository.save(Certificate.builder()
                .verificationCode(verificationCode)
                .issueDate(LocalDate.now())
                .user(user)
                .course(course)
                .build());
    }
}