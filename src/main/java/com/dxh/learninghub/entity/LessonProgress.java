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
        name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
)
public class LessonProgress extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    Lesson lesson;

    @Column(nullable = false)
    Boolean completed;
}
