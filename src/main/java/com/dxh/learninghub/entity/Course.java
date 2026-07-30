package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.CourseStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "courses",
        indexes = @Index(
                name = "idx_course_status_created",
                columnList = "status, create_at"))
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Course extends AbstractEntity<Long>{

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "description", columnDefinition = "MEDIUMTEXT")
    String description;

    @Column(name = "points", columnDefinition = "BIGINT DEFAULT 0")
    Long points;

    @Column(name = "duration")
    Integer duration; // in hours

    @Column(name = "language")
    String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    CourseLevel courseLevel;

    @Column(name = "thumbnail")
    String thumbnail;

    @Column(name = "video_url")
    String videoUrl;

    @Column(name = "total_enrollments", columnDefinition = "BIGINT DEFAULT 0")
    Long totalEnrollments;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    User author;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @Builder.Default
    Set<Enrollment> enrollments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @Builder.Default
    List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @Builder.Default
    Set<Favorite> favorites = new LinkedHashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    @JsonIgnore
    Set<Chapter> chapters = new LinkedHashSet<>();


    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CourseStatus status = CourseStatus.DRAFT;

    @PrePersist
    private void prePersist() {

        if(status==null){
            status = CourseStatus.DRAFT;
        }

        if (totalEnrollments == null) {
            totalEnrollments = 0L;
        }

        if (points == null) {
            points = 0L;
        }
    }
}
