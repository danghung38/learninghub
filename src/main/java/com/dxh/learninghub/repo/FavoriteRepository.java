package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Favorite;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @EntityGraph(attributePaths = {
            "course",
            "course.author"
    })
    Page<Favorite> findByUserAndCourseStatus(
            User user,
            CourseStatus status,
            Pageable pageable);

    Optional<Favorite> findByUserAndCourseId(User user, Long courseId);
}
