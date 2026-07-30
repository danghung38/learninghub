package com.dxh.learninghub.repo.specification;

import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseLevel;
import com.dxh.learninghub.enums.CourseStatus;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class CourseSpecification {

    private CourseSpecification() {}

    // Public Search Tối Ưu: Chỉ JOIN/FETCH 1 lần duy nhất vào bảng User (Author)
    public static Specification<Course> publicSearch(String[] courseFilters, String[] authorFilters) {
        return buildSearchSpec(courseFilters, authorFilters, CourseStatus.APPROVED);
    }

    // Admin Search Tối Ưu
    public static Specification<Course> adminSearch(String[] courseFilters, String[] authorFilters, CourseStatus status) {
        return buildSearchSpec(courseFilters, authorFilters, status);
    }

    private static Specification<Course> buildSearchSpec(String[] courseFilters, String[] authorFilters, CourseStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Kiểm tra nếu là Count Query (khi phân trang) -> Dùng JOIN thường.
            //    Nếu là Select Data Query -> Dùng FETCH JOIN để tránh N+1.
            boolean isCountQuery = Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType());

            From<Course, User> authorFrom;
            if (isCountQuery) {
                authorFrom = root.join("author", JoinType.LEFT);
            } else {
                authorFrom = (From<Course, User>) (Object) root.fetch("author", JoinType.LEFT);
            }

            // 2. Filter theo Status truyền từ tham số ngoài (nếu có - ví dụ từ hàm getByStatus)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Filter theo các thuộc tính của Course (Bao gồm cả trường hợp lọc status qua mảng courseFilters như course=status:PENDING)
            if (courseFilters != null && courseFilters.length > 0) {
                appendCoursePredicates(root, cb, courseFilters, predicates);
            }

            // 4. Filter theo Author (Tái sử dụng authorFrom đã join ở trên -> KHÔNG BỊ DUPLICATE JOIN)
            if (authorFilters != null && authorFilters.length > 0) {
                appendAuthorPredicates(authorFrom, cb, authorFilters, predicates);
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void appendCoursePredicates(jakarta.persistence.criteria.Path<Course> root,
                                               jakarta.persistence.criteria.CriteriaBuilder cb,
                                               String[] filters,
                                               List<Predicate> predicates) {
        for (String filter : filters) {
            if (!StringUtils.hasText(filter)) continue;

            String[] parts = filter.split("[:><!]", 2);
            if (parts.length < 2) continue;

            String key = parts[0].trim().toLowerCase();
            String value = parts[1].trim();
            char operator = filter.charAt(parts[0].length());

            switch (key) {
                //ĐÃ XÓA CASE "STATUS" Ở ĐÂY CHO AN TOÀN VÀ TẬP TRUNG

                case "title", "name" -> predicates.add(
                        cb.like(cb.lower(root.get("title")), "%" + value.toLowerCase() + "%")
                );
                case "level", "courselevel" -> {
                    try {
                        CourseLevel level = CourseLevel.valueOf(value.toUpperCase());
                        predicates.add(operator == '!' ? cb.notEqual(root.get("courseLevel"), level)
                                : cb.equal(root.get("courseLevel"), level));
                    } catch (IllegalArgumentException ignored) {}
                }
                case "language" -> predicates.add(
                        cb.equal(cb.lower(root.get("language")), value.toLowerCase())
                );
                case "points" -> {
                    try {
                        long numValue = Long.parseLong(value);
                        switch (operator) {
                            case '>' -> predicates.add(cb.greaterThanOrEqualTo(root.get("points"), numValue));
                            case '<' -> predicates.add(cb.lessThanOrEqualTo(root.get("points"), numValue));
                            case '!' -> predicates.add(cb.notEqual(root.get("points"), numValue));
                            default  -> predicates.add(cb.equal(root.get("points"), numValue));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private static void appendAuthorPredicates(From<Course, User> authorFrom,
                                               jakarta.persistence.criteria.CriteriaBuilder cb,
                                               String[] filters,
                                               List<Predicate> predicates) {
        for (String filter : filters) {
            if (!StringUtils.hasText(filter)) continue;

            String[] parts = filter.split("[:><!]", 2);

            // Nếu truyền kiểu cũ không có key (vd: author=Nguyễn Văn A) -> Mặc định hiểu là tìm theo tên
            if (parts.length < 2) {
                predicates.add(cb.like(cb.lower(authorFrom.get("fullName")), "%" + filter.trim().toLowerCase() + "%"));
                continue;
            }

            String key = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            switch (key) {
                case "name", "fullname" -> predicates.add(
                        cb.like(cb.lower(authorFrom.get("fullName")), "%" + value.toLowerCase() + "%")
                );
                case "expertise" -> predicates.add(
                        cb.like(cb.lower(authorFrom.get("expertise")), "%" + value.toLowerCase() + "%")
                );
            }
        }
    }
}