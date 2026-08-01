package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);

    @EntityGraph(attributePaths = {"course", "course.author"})
    Page<Certificate> findAllByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "course", "course.author"})
    Optional<Certificate> findByVerificationCode(String verificationCode);
}
