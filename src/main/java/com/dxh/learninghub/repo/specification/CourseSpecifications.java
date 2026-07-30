package com.dxh.learninghub.repo.specification;

import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.dxh.learninghub.constant.AppConstant.SEARCH_SPEC_OPERATOR;

public class CourseSpecifications {

    private static final Pattern SPEC_PATTERN = Pattern.compile(SEARCH_SPEC_OPERATOR);

    private CourseSpecifications() {}

    public static Specification<Course> publicSearch(
            String[] courseFilters,
            String[] authorFilters) {
        return Specification.where(isApproved())
                .and(buildCourseFilters(courseFilters))
                .and(buildAuthorFilters(authorFilters));
    }

    public static Specification<Course> hasStatus(CourseStatus status) {

        return (root, query, cb) -> {

            if (query.getResultType() != Long.class &&
                    query.getResultType() != long.class) {

                root.fetch("author", JoinType.LEFT);
                query.distinct(true);
            }

            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Course> isApproved() {
        return (root, query, cb) -> {
            //tránh N+1
            if (query.getResultType() != Long.class &&
                    query.getResultType() != long.class) {
                root.fetch("author", JoinType.LEFT);
            }

            return cb.equal(
                    root.get("status"),
                    CourseStatus.APPROVED
            );
        };
    }

    public static Specification<Course> hasAuthor(String authorName) {
        return (root, query, cb) -> {
            query.distinct(true);

            // Nếu là câu lệnh COUNT, chỉ cần JOIN để lọc, không FETCH dữ liệu
            if (query.getResultType() == Long.class || query.getResultType() == long.class) {
                Join<Course, User> authorJoin = root.join("author");
                return cb.like(cb.lower(authorJoin.get("fullName")), "%" + authorName.toLowerCase() + "%");
            }

            // Nếu là câu lệnh SELECT, biến JOIN thành FETCH để vừa lọc vừa lấy dữ liệu về RAM
            Fetch<Course, User> authorFetch = root.fetch("author", JoinType.LEFT);
            Join<Course, User> authorJoin = (Join<Course, User>) authorFetch;

            return cb.like(
                    cb.lower(authorJoin.get("fullName")),
                    "%" + authorName.toLowerCase() + "%"
            );
        };
    }

    private static Specification<Course> buildCourseFilters(String[] filters) {
        GenericSpecificationBuilder<Course> builder = new GenericSpecificationBuilder<>();
        match(filters)
                .filter(matcher -> !"status".equalsIgnoreCase(matcher.group(1)))
                .map(matcher -> new SpecSearchCriteria(
                        null,
                        matcher.group(1),
                        matcher.group(2),
                        matcher.group(3),
                        matcher.group(4),
                        matcher.group(5)))
                .forEach(builder::with);
        return builder.build();
    }

    private static Specification<Course> buildAuthorFilters(String[] filters) {
        return match(filters)
                .map(matcher -> hasAuthor(matcher.group(4)))
                .reduce(Specification.where(null), Specification::and);
    }

    private static Stream<Matcher> match(String[] filters) {
        return filters == null
                ? Stream.empty()
                : Arrays.stream(filters)
                        .map(SPEC_PATTERN::matcher)
                        .filter(Matcher::find);
    }
}
