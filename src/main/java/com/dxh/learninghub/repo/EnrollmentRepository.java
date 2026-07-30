package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Enrollment;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @EntityGraph(attributePaths = {"course", "course.author"})
    List<Enrollment> findCourseByUser(User user);

    boolean existsByUserAndCourse(User user, Course course);

    boolean existsByUserAndCourseAndStatusIn(
            User user, Course course, Collection<EnrollmentStatus> statuses);

    Optional<Enrollment> findByUserAndCourse(User user, Course course);

    @EntityGraph(attributePaths = {"user", "course"})
    Page<Enrollment> findByCourseIdOrderByCreatedAtDesc(
            Long courseId,
            Pageable pageable);

    @Query("""
            select count(distinct e.user.id)
            from Enrollment e
            where e.course.author.id = :teacherId
            """)
    Long countDistinctStudentsByTeacher(Long teacherId);

    @Query("""
            select count(e.id)
            from Enrollment e
            where e.course.author.id = :teacherId
            """)
    Long countEnrollmentsByTeacher(Long teacherId);

    // ===== Revenue =====

    @Query("""
            select coalesce(sum(e.spentPoints), 0)
            from Enrollment e
            where e.course.author.id = :teacherId
            """)
    Long sumSpentPointsByTeacher(@Param("teacherId") Long teacherId);

    // Aggregate theo tháng trong 1 năm -> tối đa 12 dòng
    @Query("""
            select month(e.createdAt) as m, coalesce(sum(e.spentPoints), 0) as revenue
            from Enrollment e
            where e.course.author.id = :teacherId
              and year(e.createdAt) = :year
            group by month(e.createdAt)
            order by month(e.createdAt)
            """)
    List<Object[]> sumSpentPointsByTeacherGroupByMonth(
            @Param("teacherId") Long teacherId,
            @Param("year") Integer year);

    // Aggregate theo ngày trong 1 tháng -> tối đa 31 dòng, gộp tuần ở Service
    @Query("""
            select day(e.createdAt) as d, coalesce(sum(e.spentPoints), 0) as revenue
            from Enrollment e
            where e.course.author.id = :teacherId
              and year(e.createdAt) = :year
              and month(e.createdAt) = :month
            group by day(e.createdAt)
            order by day(e.createdAt)
            """)
    List<Object[]> sumSpentPointsByTeacherGroupByDayOfMonth(
            @Param("teacherId") Long teacherId,
            @Param("year") Integer year,
            @Param("month") Integer month);
}
