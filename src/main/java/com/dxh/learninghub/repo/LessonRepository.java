package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Chapter;
import com.dxh.learninghub.entity.Lesson;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByChapterOrderByIdAsc(Chapter chapter);

    @EntityGraph(attributePaths = {
            "chapter",
            "chapter.course",
            "chapter.course.author"
    })
    Optional<Lesson> findWithCourseById(Long id);

    long countByChapterCourseId(Long courseId);
}
