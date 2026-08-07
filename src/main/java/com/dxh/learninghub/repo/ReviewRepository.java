package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Review;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByUserAndCourseAndParentReviewIsNull(User user, Course course);

    @EntityGraph(attributePaths = {"user", "course"})
    Optional<Review> findWithUserAndCourseById(Long id);

    @EntityGraph(attributePaths = "user")
    Page<Review> findByCourseIdAndParentReviewIsNull(
            Long courseId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "parentReview"})
    List<Review> findByParentReviewIdInOrderByCreatedAtAsc(
            Collection<Long> parentReviewIds);

    long countByCourseIdAndParentReviewIsNull(Long courseId);

    @Query("""
            select avg(r.rating)
            from Review r
            where r.course.id = :courseId
              and r.parentReview is null
            """)
    Double getAverageRating(@Param("courseId") Long courseId);

    @Query("""
            select r.rating, count(r.id)
            from Review r
            where r.course.id = :courseId
              and r.parentReview is null
              and r.rating is not null
            group by r.rating
            """)
    List<Object[]> getRatingDistribution(@Param("courseId") Long courseId);

    @Query("""
            select count(r.id)
            from Review r
            where r.course.author.id = :teacherId
              and r.parentReview is null
            """)
    Long countByTeacher(@Param("teacherId") Long teacherId);

    @Query("""
            select coalesce(avg(r.rating),0)
            from Review r
            where r.course.author.id = :teacherId
              and r.parentReview is null
            """)
    Double averageRatingByTeacher(@Param("teacherId") Long teacherId);
}
