package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.LessonContentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "lessons")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Lesson extends AbstractEntity<Long> {

    @Column(name = "lesson_name", nullable = false)
    String lessonName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    Chapter chapter;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    LessonContentType contentType;

    @Column(name = "content_url", nullable = false)
    String contentUrl;

    @Column(name = "description")
    String description; // Mô tả nội dung nếu cần
}
