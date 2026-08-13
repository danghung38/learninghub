package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    @EntityGraph(attributePaths = "author")
    List<Course> findByAuthorAndStatusNot(User author, CourseStatus status);

    @EntityGraph(attributePaths = "author")
    Page<Course> findByAuthorIdAndStatus(Long authorId, CourseStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<Course> findWithAuthorById(Long id);

    @Query("""
                SELECT c
                FROM Course c
                WHERE c.id = :id
                AND c.status = com.dxh.learninghub.enums.CourseStatus.APPROVED
            """)
    Optional<Course> findPublicCourseById(Long id);

    @Query("""
            SELECT c.title
            FROM Course c
            WHERE c.status = com.dxh.learninghub.enums.CourseStatus.APPROVED
            AND LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    List<String> findTitleSuggestions(String query);


    Long countCoursesByAuthorId(Long teacherId);

    @EntityGraph(attributePaths = {
            "chapters",
            "chapters.lessons",
            "author"
    })
    Optional<Course> findWithChaptersAndLessonsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c join fetch c.author where c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);
}
