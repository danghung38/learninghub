package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.CourseProgress;
import com.dxh.learninghub.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {

    Optional<CourseProgress> findByUserAndCourse(User user, Course course);
    Optional<CourseProgress> findByUserAndCourseId(User user, Long courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cp
            from CourseProgress cp
            where cp.user = :user and cp.course = :course
            """)
    Optional<CourseProgress> findByUserAndCourseForUpdate(
            @Param("user") User user,
            @Param("course") Course course);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cp
            from CourseProgress cp
            where cp.course.id = :courseId
            """)
    List<CourseProgress> findAllByCourseIdForUpdate(@Param("courseId") Long courseId);
}
