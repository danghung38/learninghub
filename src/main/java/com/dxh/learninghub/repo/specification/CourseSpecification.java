package com.dxh.learninghub.repo.specification;

import com.dxh.learninghub.dto.request.CourseSearchFilterRequest;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.CourseStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class CourseSpecification {

    private CourseSpecification() {}

    public static Specification<Course> publicSearch(CourseSearchFilterRequest filter) {
        return buildSearchSpec(filter, CourseStatus.APPROVED);
    }

    public static Specification<Course> adminSearch(CourseSearchFilterRequest filter) {
        return buildSearchSpec(filter, null);
    }

    private static Specification<Course> buildSearchSpec(CourseSearchFilterRequest filter, CourseStatus defaultStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean isCountQuery = Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType());

            From<Course, User> authorFrom;
            if (isCountQuery) {
                authorFrom = root.join("author", JoinType.LEFT);
            } else {
                Fetch<Course, User> fetch = root.fetch("author", JoinType.LEFT);
                authorFrom = (Join<Course, User>) fetch;
            }

            if (filter == null) {
                if (defaultStatus != null) {
                    predicates.add(cb.equal(root.get("status"), defaultStatus));
                }
                return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
            }

            // 1. Status Filter
            CourseStatus targetStatus = defaultStatus != null ? defaultStatus
                    : (StringUtils.hasText(filter.status()) ? CourseStatus.valueOf(filter.status().toUpperCase().trim()) : null);

            if (targetStatus != null) {
                predicates.add(cb.equal(root.get("status"), targetStatus));
            }

            // 2. Course Title Filter
            if (StringUtils.hasText(filter.title())) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + filter.title().toLowerCase().trim() + "%"));
            }

            // 3. Course Language Filter
            if (StringUtils.hasText(filter.language())) {
                predicates.add(cb.equal(cb.lower(root.get("language")), filter.language().toLowerCase().trim()));
            }

            // 4. Course Level Filter
            if (StringUtils.hasText(filter.courseLevel())) {
                try {
                    CourseLevel level = CourseLevel.valueOf(filter.courseLevel().toUpperCase().trim());
                    predicates.add(cb.equal(root.get("courseLevel"), level));
                } catch (IllegalArgumentException ignored) {}
            }

            // 5. Course Points Range Filter (minPoints & maxPoints)
            Long min = filter.minPoints();
            Long max = filter.maxPoints();

            if (min != null && max != null && min > max) {
                Long temp = min;
                min = max;
                max = temp;
            }

            if (min != null && min >= 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("points"), min));
            }
            if (max != null && max >= 0) {
                predicates.add(cb.lessThanOrEqualTo(root.get("points"), max));
            }

            // 6. Author Name Filter
            if (StringUtils.hasText(filter.authorName())) {
                predicates.add(cb.like(cb.lower(authorFrom.get("fullName")), "%" + filter.authorName().toLowerCase().trim() + "%"));
            }

            // 7. Author Expertise Filter
            if (StringUtils.hasText(filter.authorExpertise())) {
                predicates.add(cb.like(cb.lower(authorFrom.get("expertise")), "%" + filter.authorExpertise().toLowerCase().trim() + "%"));
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}