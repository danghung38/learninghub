package com.dxh.learninghub.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(
                        name = "idx_review_course_parent_created",
                        columnList = "course_id, parent_review_id, create_at")
        })
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Review extends AbstractEntity<Long> {
    @Column(nullable = false)
    String content;

    @Column(nullable = true)
    Integer rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_review_id")
    Review parentReview;

    @OneToMany(
            mappedBy = "parentReview",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    List<Review> replies = new ArrayList<>();

    public void addReply(Review reply) {
        replies.add(reply);
        reply.setParentReview(this);
        reply.setCourse(course);
    }
}
