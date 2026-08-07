package com.dxh.learninghub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "advertisements")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Advertisement extends AbstractEntity<Long> {

    @Column(nullable = false, length = 100)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false)
    String image;

    @Column(nullable = false)
    String link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    Course course;

    @Column(nullable = false)
    LocalDate startDate;

    @Column(nullable = false)
    LocalDate endDate;

    @Column(nullable = false)
    boolean active;

    /**
     * Delivery state for the current advertisement.
     *
     * This flag is the migration point for Kafka: replace the direct fan-out
     * with an advertisement event when the messaging pipeline is introduced.
     */
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    boolean sent = false;
}
