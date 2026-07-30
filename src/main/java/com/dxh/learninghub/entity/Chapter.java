package com.dxh.learninghub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "chapters")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Chapter extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    Course course;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    Set<Lesson> lessons = new LinkedHashSet<>();

    @Column(name = "title", nullable = false)
    String chapterName;

    @Column(name = "description", columnDefinition = "MEDIUMTEXT")
    String description;

}
