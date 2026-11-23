package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Chapter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    @EntityGraph(attributePaths = {"course"})
    Optional<Chapter> findById(Long id);

    @EntityGraph(attributePaths = {
            "course",
            "course.author"
    })
    Optional<Chapter> findWithCourseAndAuthorById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "course",
            "course.author"
    })
    @Query("select chapter from Chapter chapter where chapter.id = :id")
    Optional<Chapter> findWithCourseAndAuthorByIdForUpdate(@Param("id") Long id);
}
