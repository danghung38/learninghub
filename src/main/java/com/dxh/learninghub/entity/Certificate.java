package com.dxh.learninghub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "certificates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_certificate_user_course", columnNames = {"user_id", "course_id"}),
        @UniqueConstraint(name = "uk_certificate_verification_code", columnNames = "name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Certificate extends AbstractEntity<Long> {

    @Column(name = "name", nullable = false, length = 32)
    String verificationCode;

    @Column(name = "issue_date", nullable = false)
    LocalDate issueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    Course course;

    @Column(name = "object_key", nullable = false, length = 500)
    String objectKey;
}
