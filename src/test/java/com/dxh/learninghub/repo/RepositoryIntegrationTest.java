package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Enrollment;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.enums.EnrollmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired TestEntityManager entityManager;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired EnrollmentRepository enrollmentRepository;

    @Test
    void userRepository_findsByUniqueIdentityFields() {
        User user = persistUser("student01", "student01@example.com");
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findByEmail("student01@example.com"))
                .get().extracting(User::getId).isEqualTo(user.getId());
        assertThat(userRepository.existsByUsername("student01")).isTrue();
        assertThat(userRepository.findByUsernameOrEmail("missing", "student01@example.com"))
                .isPresent();
    }

    @Test
    void courseRepository_exposesOnlyApprovedCourseAsPublic() {
        User teacher = persistUser("teacher01", "teacher01@example.com");
        Course approved = persistCourse("Approved", CourseStatus.APPROVED, teacher);
        Course draft = persistCourse("Draft", CourseStatus.DRAFT, teacher);
        entityManager.flush();
        entityManager.clear();

        assertThat(courseRepository.findPublicCourseById(approved.getId())).isPresent();
        assertThat(courseRepository.findPublicCourseById(draft.getId())).isEmpty();
        assertThat(courseRepository.findTitleSuggestions("prov")).containsExactly("Approved");
    }

    @Test
    void enrollmentRepository_returnsOnlyEnrollmentsOfApprovedCourses() {
        User teacher = persistUser("teacher02", "teacher02@example.com");
        User student = persistUser("student02", "student02@example.com");
        Course approved = persistCourse("Java", CourseStatus.APPROVED, teacher);
        Course deleted = persistCourse("Legacy", CourseStatus.DELETED, teacher);
        entityManager.persist(Enrollment.builder().user(student).course(approved)
                .status(EnrollmentStatus.ACTIVE).spentPoints(100L).build());
        entityManager.persist(Enrollment.builder().user(student).course(deleted)
                .status(EnrollmentStatus.ACTIVE).spentPoints(50L).build());
        entityManager.flush();
        entityManager.clear();

        User managedStudent = entityManager.find(User.class, student.getId());
        List<Enrollment> result = enrollmentRepository.findCourseByUser(managedStudent);

        assertThat(result).singleElement()
                .extracting(enrollment -> enrollment.getCourse().getTitle())
                .isEqualTo("Java");
    }

    private User persistUser(String username, String email) {
        return entityManager.persistAndFlush(User.builder()
                .username(username).email(email).enabled(true).banned(false).points(0L).build());
    }

    private Course persistCourse(String title, CourseStatus status, User teacher) {
        return entityManager.persistAndFlush(Course.builder()
                .title(title).author(teacher).status(status).points(0L).totalEnrollments(0L).build());
    }
}
