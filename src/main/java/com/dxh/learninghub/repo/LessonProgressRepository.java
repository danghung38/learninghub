package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Lesson;
import com.dxh.learninghub.entity.LessonProgress;
import com.dxh.learninghub.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LessonProgress> findByUserAndLesson(User user, Lesson lesson);

    long countByUserIdAndLessonChapterCourseIdAndCompletedTrue(
            Long userId,
            Long courseId);

    @Query("""
            select lp.lesson.id
            from LessonProgress lp
            where lp.user.id = :userId
              and lp.lesson.chapter.course.id = :courseId
              and lp.completed = true
            order by lp.lesson.id
            """)
    List<Long> findCompletedLessonIds(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId);

    @Modifying
    @Query("""
            delete from LessonProgress lp
            where lp.lesson.id = :lessonId
            """)
    int deleteAllByLessonId(@Param("lessonId") Long lessonId);

    @Modifying
    @Query("""
            delete from LessonProgress lp
            where lp.lesson.chapter.id = :chapterId
            """)
    int deleteAllByChapterId(@Param("chapterId") Long chapterId);
}
