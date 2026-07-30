package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Chapter;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
