package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Entity
@Table(name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_course",
                        columnNames = {"user_id", "course_id"}
                )
        },
        indexes = @Index(
                name = "idx_enrollment_course_created",
                columnList = "course_id, create_at"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Enrollment extends AbstractEntity<Long>{

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    EnrollmentStatus status;

    @Column(name = "spent_points", nullable = false)
    Long spentPoints;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = EnrollmentStatus.PENDING;
        }
    }

}
