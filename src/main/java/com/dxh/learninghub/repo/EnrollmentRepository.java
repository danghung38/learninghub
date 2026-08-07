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
    @Query("""
            select e
            from Enrollment e
            where e.user = :user
              and e.course.status = com.dxh.learninghub.enums.CourseStatus.APPROVED
            order by e.createdAt desc
            """)
    List<Enrollment> findCourseByUser(@Param("user") User user);

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

    @Query("""
            select coalesce(sum(e.spentPoints), 0)
            from Enrollment e
            """)
    Long sumSpentPointsForAdmin();

    @Query("""
            select count(e.id)
            from Enrollment e
            """)
    Long countAllEnrollments();

    @Query("""
            select month(e.createdAt), coalesce(sum(e.spentPoints), 0), count(e.id)
            from Enrollment e
            where year(e.createdAt) = :year
            group by month(e.createdAt)
            order by month(e.createdAt)
            """)
    List<Object[]> sumSpentPointsGroupByMonthForAdmin(@Param("year") Integer year);

    @Query("""
            select day(e.createdAt), coalesce(sum(e.spentPoints), 0), count(e.id)
            from Enrollment e
            where year(e.createdAt) = :year
              and month(e.createdAt) = :month
            group by day(e.createdAt)
            order by day(e.createdAt)
            """)
    List<Object[]> sumSpentPointsGroupByDayOfMonthForAdmin(
            @Param("year") Integer year,
            @Param("month") Integer month);

    // Aggregate by month within a year -> at most 12 rows
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

    // Aggregate by day within a month -> at most 31 rows, grouped into weeks in the service
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
