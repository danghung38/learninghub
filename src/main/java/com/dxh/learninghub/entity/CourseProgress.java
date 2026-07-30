package com.dxh.learninghub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "course_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"})
)
public class CourseProgress extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    Course course;

    @Column(name = "completed_lessons", nullable = false)
    Integer completedLessons;

    @Column(name = "total_lessons", nullable = false)
    Integer totalLessons;

    @Column(name = "progress_percent", nullable = false)
    Integer progressPercent;

    @Column(nullable = false)
    Boolean completed;

    @PrePersist
    public void prePersist() {
        if (completedLessons == null) completedLessons = 0;
        if (progressPercent == null) progressPercent = 0;
        if (completed == null) completed = false;
    }
}
